/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.impl

import kotlinx.coroutines.flow.firstOrNull
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.EntityReferenceLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.be.ehealth.EfactLogic
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.entities.embed.TelecomType
import org.taktik.icure.services.external.rest.v1.dto.be.efact.*
import org.taktik.icure.services.external.rest.v1.mapper.MessageMapper
import org.taktik.icure.services.external.rest.v1.mapper.PatientMapper
import java.math.BigInteger
import java.util.Arrays
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToLong

@Profile("kmehr")
@Service
class EfactLogicImpl(
    private val entityReferenceLogic: EntityReferenceLogic,
    private val sessionLogic: SessionInformationProvider,
    val healthcarePartyLogic: HealthcarePartyLogic,
    val patientLogic: PatientLogic,
    private val insuranceLogic: InsuranceLogic,
    private val patientMapper: PatientMapper,
    private val messageMapper: MessageMapper,
) : EfactLogic {

    private fun encodeShortRefFromUUID(uuid: UUID): String {
        val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)

        return BigInteger(1, bb.array().sliceArray(0 until 8)).toString(36).padStart(13, '0')
    }

    private fun encodeRefFromUUID(uuid: UUID): String {
        val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)

        return BigInteger(1, bb.array()).toString(36)
    }

    private fun encodeNumberFromUUID(uuid: UUID): Long {
        val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)

        return BigInteger(1, Arrays.copyOfRange(bb.array(), 0, 4)).toLong()
    }

    /**
     * Creates a new batch of invoices
     * @param sendNumber the last 3 digits of the insurance code
     * @param messageId a UUID that represents the batch
     * @param insurance the insurance related to the invoice
     * @param ivs a map where each key is the ID of a Patient and each value is a list of invoices
     * @param hcp the HCP creating the batch
     * @return an InvoicesBatch
     */
    private suspend fun createBatch(sendNumber: Int, messageId: String, insurance: Insurance, ivs: Map<String, List<Invoice>>, hcp: HealthcareParty): InvoicesBatch {
        if(hcp.cbe == null) throw IllegalArgumentException("The CBE of the HealthcareParty must not be null")
        if(hcp.nihii == null) throw IllegalArgumentException("The NIHII of the HealthcareParty must not be null")

        val bic = hcp.financialInstitutionInformation.find { fi -> insurance.code == fi.key && !(fi.proxyBic.isNullOrBlank() && fi.bic.isNullOrBlank()) && !(fi.bankAccount.isNullOrBlank() && fi.proxyBankAccount.isNullOrBlank()) }?.let { it.proxyBic?.toNullIfBlank() ?: it.bic?.toNullIfBlank() }
            ?: hcp.proxyBic?.toNullIfBlank() ?: hcp.bic?.toNullIfBlank() ?: hcp.financialInstitutionInformation.find { fi -> !(fi.proxyBic.isNullOrBlank() && fi.bic.isNullOrBlank()) && !(fi.bankAccount.isNullOrBlank() && fi.proxyBankAccount.isNullOrBlank()) }?.let { it.proxyBic?.toNullIfBlank() ?: it.bic?.toNullIfBlank() }
            ?: throw IllegalArgumentException("Missing BIC in bank account information")

        val iban = hcp.financialInstitutionInformation.find { fi -> insurance.code == fi.key && !(fi.proxyBic.isNullOrBlank() && fi.bic.isNullOrBlank()) && !(fi.bankAccount.isNullOrBlank() && fi.proxyBankAccount.isNullOrBlank()) }?.let { it.proxyBankAccount?.toNullIfBlank() ?: it.bankAccount?.toNullIfBlank() }
            ?: hcp.proxyBankAccount?.toNullIfBlank() ?: hcp.bankAccount?.toNullIfBlank() ?: hcp.financialInstitutionInformation.find { fi -> !(fi.proxyBic.isNullOrBlank() && fi.bic.isNullOrBlank()) && !(fi.bankAccount.isNullOrBlank() && fi.proxyBankAccount.isNullOrBlank()) }?.let { it.proxyBankAccount?.toNullIfBlank() ?: it.bankAccount?.toNullIfBlank() }
            ?: throw IllegalArgumentException("Missing IBAN in bank account information")

        val calendar = Calendar.getInstance()

        val longRef = encodeRefFromUUID(UUID.fromString(messageId))
        val shortRef = encodeShortRefFromUUID(UUID.fromString(messageId))

        return InvoicesBatch(
            invoicingYear = calendar.get(Calendar.YEAR),
            invoicingMonth = calendar.get(Calendar.MONTH) + 1,
            uniqueSendNumber = sendNumber.toLong(),
            batchRef = "" + longRef,
            fileRef = "" + shortRef,
            ioFederationCode = insurance.code,
            numericalRef = calendar.get(Calendar.YEAR).toLong() * 1000000 + insurance.code!!.toLong() * 1000 + sendNumber,
            sender = InvoiceSender(
                nihii = java.lang.Long.valueOf(hcp.nihii!!.replace("[^0-9]".toRegex(), "")),
                ssin = hcp.ssin!!.replace("[^0-9]".toRegex(), ""),
                bic = bic,
                iban = iban,
                firstName = hcp.firstName,
                lastName = hcp.lastName,
                phoneNumber = (
                        hcp.addresses.map { a -> a.telecoms.first { t -> t.telecomType == TelecomType.phone } }
                            .map { t -> t.telecomNumber?.replace("\\+".toRegex(), "00")?.replace("[^0-9]".toRegex(), "") }
                            .firstOrNull() ?: "0"
                        ).toLong(),
                bce = hcp.cbe!!.replace("[^0-9]".toRegex(), "").toLong(),
                conventionCode = if (hcp.convention != null) hcp.convention else 0
            ),
            invoices = ivs.toList().flatMap { (key, invGroup) ->
                val patient = patientLogic.getPatient(key)!!
                invGroup.map { iv ->
                    val ivcs = iv.invoicingCodes
                    val invoiceNumber = if (iv.invoiceReference != null && iv.invoiceReference!!.matches("^[0-9]{4,12}$".toRegex())) iv.invoiceReference!!.toLong() else this.encodeNumberFromUUID(UUID.fromString(iv.id))

                    val items = ivcs.fold(emptyList<InvoiceItem>()) { acc, ivc ->
                        val patientIntervention = ivc.patientIntervention ?: 0.0
                        val doctorSupplement = ivc.doctorSupplement ?: 0.0
                        val reimbursement = ivc.reimbursement ?: 0.0

                        acc + createInvoiceItem(
                            hcp,
                            encodeRefFromUUID(UUID.fromString(ivc.id)),
                                ivc.code?.toLong() ?: ivc.tarificationId?.split("\\|".toRegex())?.get(1)?.toLong() ?: throw IllegalArgumentException("Wrong code"),
                            (reimbursement * 100).roundToLong(),
                            (patientIntervention * 100).roundToLong(),
                            (doctorSupplement * 100).roundToLong(),
                            ivc.contract,
                            ivc.dateCode,
                            ivc.eidReadingHour,
                            ivc.eidReadingValue,
                            ivc.side ?: -1,
                            ivc.override3rdPayerCode,
                            ivc.timeOfDay ?: -1,
                            ivc.cancelPatientInterventionReason,
                            if (ivc.relatedCode == null) 0 else java.lang.Long.valueOf(ivc.relatedCode),
                            iv.gnotionNihii,
                            ivc.prescriberNihii,
                            ivc.units ?: 1,
                            ivc.prescriberNorm ?: -1,
                            ivc.percentNorm ?: -1,
                        )
                    }

                    EfactInvoice(
                        patient = patientMapper.map(patient),
                        ioCode = patient.insurabilities.firstOrNull()?.insuranceId?.let { insuranceLogic.getInsurance(it)?.let { ins -> ins.parent?.let { parent -> insuranceLogic.getInsurance(parent)?.code?.substring(0, 3) } } },
                        invoiceNumber = invoiceNumber,
                        invoiceRef = encodeRefFromUUID(UUID.fromString(iv.id)),
                        reason = InvoicingTreatmentReasonCode.Other,
                        items = items.toMutableList()
                    )
                }
            }

        )
    }

    /**
     * Instantiates an InvoiceItem
     */
    private fun createInvoiceItem(
        hcp: HealthcareParty,
        ref: String,
        codeNomenclature: Long,
        reimbursedAmount: Long,
        patientFee: Long,
        doctorSupplement: Long?,
        contract: String?,
        date: Long?,
        eidReading: Int?,
        eidValue: String?,
        side: Int?,
        thirdPayerExceptionCode: Int?,
        timeOfDay: Int,
        personalInterventionCoveredByThirdPartyCode: Int?,
        prestationRelative: Long?,
        dmgReference: String?,
        prescriberIdentificationNumber: String?,
        units: Int?,
        prescriberCode: Int?,
        percentNorm: Int?,
    ) = InvoiceItem(
        insuranceRef = contract,
        insuranceRefDate = date,
        dateCode = date,
        codeNomenclature = codeNomenclature,
        relatedCode = prestationRelative,
        gnotionNihii = dmgReference,
        doctorIdentificationNumber = hcp.nihii,
        doctorSupplement = doctorSupplement ?: 0,
        invoiceRef = ref,
        patientFee = patientFee,
        personalInterventionCoveredByThirdPartyCode = personalInterventionCoveredByThirdPartyCode,
        prescriberNihii = prescriberIdentificationNumber,
        reimbursedAmount = reimbursedAmount,
        override3rdPayerCode = thirdPayerExceptionCode,
        units = units ?: 0,
        prescriberNorm = prescriberCode?.let { InvoicingPrescriberCode.withCode(it) },
        percentNorm = percentNorm?.let { InvoicingPercentNorm.withCode(it) },
        sideCode = side?.let { InvoicingSideCode.withSide(it) },
        timeOfDay = InvoicingTimeOfDay.withCode(timeOfDay),
        eidItem = EIDItem(readDate = date, readHour = eidReading, readValue = eidValue)
            .takeIf { eidReading != null && eidReading != 0 && eidValue != null && eidValue.length > 6 }
    )

    override suspend fun prepareBatch(messageId: String, hcp: HealthcareParty, insurance: Insurance, invoices: Map<String, List<Invoice>>): MessageWithBatch? {
        // Retrieves the latest efact invoice for that hcp with that insurance
        val prefix = "efact:${hcp.id}:${insurance.code}:"
        val latestPrefix = entityReferenceLogic.getLatest(prefix)
        // Adds one to the prefix (new invoice)
        val entityRefId = prefix + ("" + (((latestPrefix?.id?.substring(prefix.length)?.toLong() ?: 0) + 1) % 1000000000)).padStart(9 /*1 billion invoices that are going to be mod 1000*/, '0')
        // Save the reference of this new invoice
        val entityRefs = entityReferenceLogic.createEntities(
            listOf(
                EntityReference(
                    id = entityRefId,
                    docId = messageId,
                ),
            ),
        )

        return entityRefs.firstOrNull()?.let { ref ->
            val sendNumber = ref.id.split(":").last()
            val shortSendNumber = sendNumber.toInt() % 1000
            // Creates the invoices batch and adds a delegation for the
            val invBatch = createBatch(shortSendNumber, messageId, insurance, invoices, hcp)
            val delegations = mapOf(hcp.id to setOf<Delegation>())

            // Creates a new message associated to the invoice batch
            val mm = Message(
                id = messageId,
                invoiceIds = invoices.values.flatMap { invoice -> invoice.map { it.id } }.toSet(),
                subject = "Facture tiers payant",
                status = Message.STATUS_UNREAD or Message.STATUS_EFACT or Message.STATUS_SENT,
                transportGuid = "EFACT:BATCH:${invBatch.numericalRef}",
                author = sessionLogic.getCurrentUserId(),
                responsible = hcp.id,
                fromHealthcarePartyId = hcp.id,
                recipients = setOf(insurance.id),
                externalRef = ("" + shortSendNumber).padStart(3, '0'),
                metas = mapOf(
                    "ioFederationCode" to (invBatch.ioFederationCode ?: ""),
                    "numericalRef" to (invBatch.numericalRef?.toString() ?: ""),
                    "invoiceMonth" to (invBatch.numericalRef?.toString() ?: ""),
                    "invoiceYear" to (invBatch.invoicingYear.toString()),
                    "totalAmount" to (invoices.values.sumOf {
                        it.sumOf { invoice ->
                            invoice.invoicingCodes.sumOf { code ->
                                code.reimbursement ?: 0.0
                            }
                        }
                    }).toString(),
                ),
                delegations = delegations,
                toAddresses = setOf(
                    insurance.address.telecoms.firstOrNull { t: Telecom -> t.telecomType == TelecomType.email && t.telecomNumber?.isNotEmpty() ?: false }?.telecomNumber
                        ?: insurance.code ?: "N/A",
                ),
                sent = System.currentTimeMillis(),
            )

            MessageWithBatch(
                invoicesBatch = invBatch,
                message = messageMapper.map(mm)
            )
        }
    }

}

private fun String.toNullIfBlank(): String? = this.ifBlank { null }
