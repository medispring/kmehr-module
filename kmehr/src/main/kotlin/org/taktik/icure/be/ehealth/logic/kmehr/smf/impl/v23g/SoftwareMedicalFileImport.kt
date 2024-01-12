/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.smf.impl.v23g

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.taktik.commons.uti.UTI
import org.taktik.commons.uti.impl.SimpleUTIDetector
import org.taktik.couchdb.exception.UpdateConflictException
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.FormTemplateLogic
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.Utils
import org.taktik.icure.be.ehealth.logic.kmehr.validNihiiOrNull
import org.taktik.icure.be.ehealth.logic.kmehr.validSsinOrNull
import org.taktik.icure.db.equals
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.CheckSMFPatientResult
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.domain.result.MimeAttachment
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.AddressType
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Duration
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Insurability
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.entities.embed.Medication
import org.taktik.icure.entities.embed.Medicinalproduct
import org.taktik.icure.entities.embed.PatientHealthCareParty
import org.taktik.icure.entities.embed.PlanOfAction
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.ServiceLink
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.entities.embed.Substanceproduct
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.entities.embed.TelecomType
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDADDRESSschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTYschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDINCAPACITY
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDLNKvalues
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDSEXvalues
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDTELECOMschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.LnkType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTYschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.IDINSURANCEschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENTschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.AddressTypeBase
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.HeadingType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.ItemType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.PersonType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.toInputStream
import org.taktik.icure.utils.xor
import java.io.ByteArrayInputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.time.temporal.ChronoUnit
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement

/**
 * This class provides all the method to convert a SMF (Software Medical File) KmEHR message type to the iCure Data
 * Model.
 */
@Profile("kmehr")
@org.springframework.stereotype.Service
class SoftwareMedicalFileImport(
    val patientLogic: PatientLogic,
    val userLogic: UserLogic,
    val healthcarePartyLogic: HealthcarePartyLogic,
    val healthElementLogic: HealthElementLogic,
    val contactLogic: ContactLogic,
    val documentLogic: DocumentLogic,
    private val formLogic: FormLogic,
    private val formTemplateLogic: FormTemplateLogic,
    private val insuranceLogic: InsuranceLogic,
    private val idGenerator: UUIDGenerator,
) {

    private val defaultMapping: Map<String, List<ImportMapping>> = ObjectMapper().let { om ->
        val txt = this.javaClass.classLoader.getResourceAsStream("org/taktik/icure/be/ehealth/logic/kmehr/smf/impl/smf.labels.json")
            ?.readBytes()?.toString(Charsets.UTF_8) ?: "{}"
        om.readValue(txt)
    }
    private val heItemTypes: List<String> = listOf("healthcareelement", "adr", "allergy", "socialrisk", "risk", "professionalrisk", "familyrisk", "healthissue")

    /**
     * Converts a SMF (Software Medical File) XML message to a set of Contacts, SubContacts, Services, HealthElements,
     * Documents, and Forms of the iCure Data Model.
     * @param inputData the ByteArray containing the XML message.
     * @param author the User responsible for the import.
     * @param language the language of the Content inside the iCure entities.
     * @param saveToDatabase whether to save in the database the new entities created.
     * @param mappings
     * @param dest the Patient object of the SMF.
     * @return a List of ImportResult.
     */
    suspend fun importSMF(
        inputData: ByteArray,
        author: User,
        language: String,
        saveToDatabase: Boolean,
        mappings: Map<String, List<ImportMapping>>,
        dest: Patient? = null,
    ): List<ImportResult> {
        val jc = JAXBContext.newInstance(Kmehrmessage::class.java)
        val inputStream = ByteArrayInputStream(inputData)
        val unmarshaller = jc.createUnmarshaller()
        val kmehrMessage = unmarshaller.unmarshal(inputStream) as Kmehrmessage

        val myMappings = defaultMapping + mappings

        val kmehrIndex = kmehrMessage.performIndexation(idGenerator)

        return kmehrMessage.folders.fold(emptyList()) { importResults, folder ->
            val res = ImportResult()
            kmehrMessage.header.sender.hcparties?.forEach {
                createOrProcessHcp(it, saveToDatabase, res)
            }

            val insurabilities = folder.transactions
                ?.flatMap { it.findItems { item -> item.cds.find { cds -> cds.s == CDITEMschemes.CD_ITEM }?.value == "insurancystatus" } }
                ?.firstOrNull()?.let {
                    parseInsurancyStatus(it)
                } ?: listOf()

            createOrProcessPatient(folder.patient, author, res, saveToDatabase, dest, insurabilities)?.let { patient ->
                res.patient = patient
                res.ctcs = folder.transactions
                    .filter { !kmehrIndex.isChildTransaction(it) }
                    .mapNotNull { trn -> parseTransaction(trn, author, res, language, myMappings, saveToDatabase, kmehrIndex) }
                    .toMutableList()
                res.forms.forEach {
                    if (saveToDatabase) {
                        formLogic.createForm(it)
                    }
                }

                res.patient = res.patient?.let {
                    it.copy(
                        patientHealthCareParties = it.patientHealthCareParties + res.hcps.distinctBy { hcp -> hcp.id }.map { hcp ->
                            PatientHealthCareParty(
                                healthcarePartyId = hcp.id,
                            )
                        }
                    )
                }
            }
            importResults + res
        }
    }

    /**
     * Parses a transaction and creates an iCure Contact according to the Transaction Scheme code value.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param res the ongoing ImportResult.
     * @param language the language of the content of Contact and Services.
     * @param myMappings a map that elates MF-ID to ImportMappings.
     * @param saveToDatabase whether to save the new Contact to the db.
     * @param kmehrIndex the Kmehr Message Index.
     * @return a Contact
     */
    private suspend fun parseTransaction(
        trn: TransactionType,
        author: User,
        res: ImportResult,
        language: String,
        myMappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex
    ): Contact =
        when (trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value) {
            "contactreport" -> parseContactReport(trn, author, res, language, myMappings, saveToDatabase, kmehrIndex)
            "clinicalsummary" -> parseClinicalSummary(trn, author, res, language, myMappings, saveToDatabase, kmehrIndex)
            "labresult", "result", "note", "prescription", "report" -> parseDocumentInTransaction(trn, author, res, language, myMappings, saveToDatabase, kmehrIndex)
            "pharmaceuticalprescription" -> parsePharmaceuticalPrescription(trn, author, res, language, myMappings, saveToDatabase, kmehrIndex)
            else -> parseGenericTransaction(trn, author, res, language, myMappings, saveToDatabase, kmehrIndex)
        }.let { con ->
            if (saveToDatabase) {
                try {
                    contactLogic.createContact(con)
                } catch (e: UpdateConflictException) {
                    contactLogic.createContact(con.copy(id = idGenerator.newGUID().toString())) // This happens when the Kmehr file is corrupted
                } ?: throw IllegalStateException("Cannot save contact")
            } else {
                con
            }
        }

    /**
     * Checks if all the patients in a SMF message exist in the db.
     * @param inputData the SMF message as a Flow of Byte Buffer.
     * @param author the User responsible for the check.
     * @param dest if not null, no Patient is retrieved from the db and this is used.
     * @return a List of CheckSMFPatientResult
     */
    suspend fun checkIfSMFPatientsExists(
        inputData: Flow<ByteBuffer>,
        author: User,
        dest: Patient? = null,
    ): List<CheckSMFPatientResult> {
        val jc = JAXBContext.newInstance(Kmehrmessage::class.java)

        val inputStream = inputData.toInputStream()

        val unmarshaller = jc.createUnmarshaller()
        val kmehrMessage = unmarshaller.unmarshal(inputStream) as Kmehrmessage

        val fakeResult = ImportResult()

        return kmehrMessage.folders.fold(emptyList()) { acc, folder ->
           acc + checkIfPatientExists(folder.patient, author, fakeResult, dest)
        }
    }

    /**
     * Parses a transaction and creates an iCure Contact.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ongoing ImportResult.
     * @param language the language of the content of Contact and Services.
     * @param mappings a map that elates MF-ID to ImportMappings.
     * @param saveToDatabase whether to save the new Contact to the db.
     * @param kmehrIndex the Kmehr Message Index.
     * @return a Contact
     */
    private suspend fun parseContactReport(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex,
    ): Contact = parseGenericTransaction(trn, author, v, language, mappings, saveToDatabase, kmehrIndex)

    /**
     * Parses a transaction and creates an iCure Contact.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ongoing ImportResult.
     * @param language the language of the content of Contact and Services.
     * @param mappings a map that elates MF-ID to ImportMappings.
     * @param saveToDatabase whether to save the new Contact to the db.
     * @param kmehrIndex the Kmehr Message Index.
     * @return a Contact
     */
    private suspend fun parseClinicalSummary(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex,
    ): Contact {
        return parseGenericTransaction(trn, author, v, language, mappings, saveToDatabase, kmehrIndex)
    }

    /**
     * Parses all the prescription in the transaction and adds to SubContacts.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ImportResult.
     * @param language the language to set in the Contents.
     * @param mappings
     * @param saveToDatabase whether to save the new entities to the db.
     * @param kmehrIndex the KmehrMessageIndex.
     * @return an iCureContact.
     */
    private suspend fun parsePharmaceuticalPrescription(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex,
    ): Contact {
        val transactionMfid = getTransactionMFID(trn)
        val trnAuthorHcpId = extractTransactionAuthor(trn, saveToDatabase, author, v)

        val contactId = transactionMfid?.let { kmehrIndex.transactionIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString()
        val formId = kmehrIndex.formIdMask.xor(UUID.fromString(contactId)).toString()

        val serviceAndSubContacts = trn
            .findItems { it: ItemType -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "medication" } }
            .map { item ->
                val mfId = getItemMFID(item)
                val service = parseGenericItem("treatment", "Prescription", item, author, trnAuthorHcpId, language, kmehrIndex)
                service to makeSubContact(contactId, formId, mfId, service, kmehrIndex)
            }
        val contactDate = extractTransactionDateTime(trn)

        val simplifiedSubContacts = simplifySubContacts(serviceAndSubContacts.flatMap { it.second!! }).toSet()
        if (simplifiedSubContacts.isNotEmpty()) {
            v.forms.addAll(
                simplifiedSubContacts
                    .filter { sc -> !v.forms.any { it.id == sc.formId } && sc.services.isNotEmpty() }
                    .mapNotNull { it.formId }
                    .toSet()
                    .map { newFormId ->
                        Form(
                            id = newFormId,
                            parent = kmehrIndex.transactionChildOf[transactionMfid]
                                    ?.firstOrNull()
                                    ?.let { kmehrIndex.transactionIds[it]?.first?.let { cid -> kmehrIndex.formIdMask.xor(cid).toString() } }
                                    ?.takeIf { newFormId == formId },
                            contactId = contactId,
                            author = author.id,
                            responsible = trnAuthorHcpId,
                            created = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                            modified = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                        )
                    }
            )
        }

        return Contact(
            id = contactId,
            author = author.id,
            responsible = trnAuthorHcpId,
            services = serviceAndSubContacts.map { it.first }.toSet() +
                    (transactionMfid?.let {
                        kmehrIndex.parentOf[it]
                            ?.flatMap { id ->
                                kmehrIndex.transactionIds[id]?.second
                                    ?.let { transactionType ->
                                        parseTransaction(transactionType, author, v, language, mappings, saveToDatabase, kmehrIndex).services
                                    } ?: setOf() }?.toSet()
                    } ?: setOf()),
            subContacts = simplifiedSubContacts,
            openingDate = contactDate,
            closingDate = trn.isIscomplete.let { if (it) contactDate else null },
        )
    }

    /**
     * Extracts the author HCP from the TransactionType passed as parameter and creates it if it does not exist in iCure.
     * If the TransactionType contains no HCP, it uses the HCP id of the author of the import.
     * @param trn The TransactionType.
     * @param saveToDatabase Whether to save the found HCP to the db if it does not exist.
     * @param author The author of the import.
     * @param v the ImportResult in progress.
     * @return the HCP id.
     */
    private suspend fun extractTransactionAuthor(trn: TransactionType, saveToDatabase: Boolean, author: User, v: ImportResult) =
        trn.author?.hcparties?.filter { it.cds.any { cds -> cds.s == CDHCPARTYschemes.CD_HCPARTY } }
            ?.firstNotNullOfOrNull {
                createOrProcessHcp(it, saveToDatabase, v)
            }?.id ?: author.healthcarePartyId ?: throw IllegalArgumentException("The author's healthcarePartyId must be set")

    /**
     * Extracts the item representing the date and time of the transaction and converts it into a fuzzy date.
     * @param trn the TransactionType.
     * @return A fuzzy date.
     */
    private fun extractTransactionDateTime(trn: TransactionType) =
        trn.findItem { it.cds.any { cds -> cds.s == CDITEMschemes.CD_ITEM && cds.value == "encounterdatetime" } }
            ?.let {
                it.contents?.find { c -> c.date != null }?.let { c -> Utils.makeFuzzyLongFromDateAndTime(c.date, c.time) }
            } ?: trn.date?.let { Utils.makeFuzzyLongFromDateAndTime(it, trn.time) }

    /**
     * Creates an iCure SubContact for all the items in the KmEHR index with the given MF-ID that also have ItemTypes
     * with the same id.
     * @param contactId the id of the Contact parent of the SubContacts to create.
     * @param formId the id of the Form to assign to the SubContacts.
     * @param mfId the MF-ID of the SMF.
     * @param service the Service linked to the SubContacts.
     * @param kmehrIndex the KmehrMessageIndex.
     * @return a List of SubContacts or null.
     */
    private fun makeSubContact(contactId: String, formId: String?, mfId: String?, service: Service, kmehrIndex: KmehrMessageIndex): List<SubContact>? =
        kmehrIndex.serviceFor[mfId]
            ?.mapNotNull { mf -> kmehrIndex.itemIds[mf]?.let { (mf to it) } }
            ?.map { (heOrHcaMfid, heOrHcaPair) ->
                val item = heOrHcaPair.second
                if (item.cds.find { it.s == CDITEMschemes.CD_ITEM }?.value == "healthcareapproach") {
                    val heId = kmehrIndex.approachFor[heOrHcaMfid]?.firstNotNullOfOrNull { kmehrIndex.itemIds[it] }?.first
                    SubContact(
                        id = UUID.nameUUIDFromBytes(("$contactId|$heId|${heOrHcaPair.first}|null").toByteArray()).toString(),
                        formId = formId,
                        healthElementId = heId?.toString(),
                        planOfActionId = heOrHcaPair.first.toString(),
                        services = listOf(ServiceLink(serviceId = service.id)),
                    )
                } else {
                    SubContact(
                        id = UUID.nameUUIDFromBytes(("$contactId|null|${heOrHcaPair.first}|null").toByteArray()).toString(),
                        formId = formId,
                        healthElementId = heOrHcaPair.first.toString(),
                        services = listOf(ServiceLink(serviceId = service.id)),
                    )
                }
        } ?: formId?.let {
            listOf(
                SubContact(
                    id = UUID.nameUUIDFromBytes(("$contactId|null|null|$formId").toByteArray()).toString(),
                    formId = formId,
                    services = listOf(ServiceLink(serviceId = service.id)),
                ),
            )
        }

    /**
     * Converts all the LnkType in the transaction to iCure Documents with attachments and saves them to the db.
     * Then creates a new contact with the remaining data and returns it.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the current ImportResult.
     * @param language the language of the content.
     * @param mappings
     * @param saveToDatabase whether to save the new Documents and Contacts to the db.
     * @param kmehrIndex the KmehrMessageIndex.
     * @return an iCure Contact.
     */
    private suspend fun parseDocumentInTransaction(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex,
    ): Contact {
        val transactionMfid = getTransactionMFID(trn)
        val trnAuthorHcpId = extractTransactionAuthor(trn, saveToDatabase, author, v)
        val trnTypeCd = trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION_TYPE }?.value

        val services = trn.headingsAndItemsAndTexts
            ?.filterIsInstance(LnkType::class.java)
            ?.filter { it.type == CDLNKvalues.MULTIMEDIA && it.url == null }
            ?.map { lnk ->
                val docName = trn.cds.firstOrNull { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.dn ?: trnTypeCd ?: "unnamed_document"
                val svcRecordDateTime = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli()

                val serviceId = idGenerator.newGUID().toString()
                val documentId = idGenerator.newGUID().toString()

                lnk.mediatype?.value()?.let {
                    v.attachments.put(
                        documentId,
                        MimeAttachment(data = lnk.value),
                    )
                }

                val (mainUti, otherUtis) = extractUtis(lnk)
                val valueDate = extractTransactionDateTime(trn)

                Service(
                    id = serviceId,
                    label = (trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value) ?: "document",
                    tags = setOf(CodeStub.from("CD-ITEM-EXT", "document", "1")),
                    valueDate = valueDate,
                    openingDate = valueDate,
                    qualifiedLinks = transactionMfid
                        ?.let { id -> kmehrIndex.itemIds[id]?.first?.toString()
                            ?.let { mapOf(LinkQualification.relatedService to mapOf(UUID.randomUUID().toString() to it)) }
                        } ?: mapOf(),
                    content = mapOf(
                        language to Content(
                            stringValue = docName,
                            documentId = Document(
                                id = documentId,
                                author = author.id,
                                responsible = trn.author?.hcparties?.filter { it.cds.any { cd -> cd.s == CDHCPARTYschemes.CD_HCPARTY && cd.value == "persphysician" } }
                                    ?.firstNotNullOfOrNull {
                                        createOrProcessHcp(it, saveToDatabase, v)
                                    }?.id ?: author.healthcarePartyId,
                                created = svcRecordDateTime,
                                modified = svcRecordDateTime,
                                name = docName,
                                mainUti = mainUti, // Keep as they are needed for patching
                                otherUtis = otherUtis
                            ).let {
                                v.documents.add(it)
                                if (saveToDatabase) {
                                    documentLogic.createDocument(it, true)?.let { doc ->
                                        documentLogic.updateAttachments(
                                            doc,
                                            mainAttachmentChange = DataAttachmentChange.CreateOrUpdate(
                                                flowOf(DefaultDataBufferFactory.sharedInstance.wrap(lnk.value)),
                                                lnk.value.size.toLong(),
                                                listOf(mainUti) + otherUtis,
                                            ),
                                        )
                                    }
                                } else it
                            }?.id,
                        ),
                    ),
                )
        } ?: listOf()

        val contactDate = extractTransactionDateTime(trn)
        val trnCd = trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value
        val contactId = transactionMfid?.let { kmehrIndex.transactionIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString()
        val formId = kmehrIndex.formIdMask.xor(UUID.fromString(contactId)).toString()
        val subContacts = services.map { makeSubContact(contactId, formId, transactionMfid, it, kmehrIndex) }
        val simplifiedSubContacts = simplifySubContacts(subContacts.flatMap { it!! }).toSet()
        if (simplifiedSubContacts.isNotEmpty()) {
            v.forms.addAll(
                simplifiedSubContacts
                    .filter { sc -> !v.forms.any { it.id == sc.formId } && sc.services.isNotEmpty() }
                    .map { it.formId ?: idGenerator.newGUID().toString() }
                    .toSet()
                    .map {
                        Form(
                            id = it,
                            parent = if (it == formId) kmehrIndex.transactionChildOf[transactionMfid]?.firstOrNull()?.let { kmehrIndex.transactionIds[it]?.first?.let { cid -> kmehrIndex.formIdMask.xor(cid).toString() } } else null,
                            contactId = contactId,
                            author = author.id,
                            responsible = trnAuthorHcpId,
                            created = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                            modified = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                        )
                }
            )
        }

        return Contact(
            id = transactionMfid?.let { kmehrIndex.transactionIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString(),
            author = author.id,
            tags = listOfNotNull(trnCd, trnTypeCd).map { CodeStub.from("CD-TRANSACTION", it, "1.0") }.toSet(),
            responsible = trn.author?.hcparties
                ?.filter { it.cds.any { cd -> cd.s == CDHCPARTYschemes.CD_HCPARTY && cd.value == "persphysician" } }
                ?.firstNotNullOfOrNull {
                    createOrProcessHcp(it, saveToDatabase, v)
                }?.id ?: author.healthcarePartyId,
            services = services.toSet() +
                    (transactionMfid?.let { mfId ->
                        kmehrIndex.parentOf[mfId]
                            ?.flatMap { kmehrIndex.transactionIds[it]?.second
                                ?.let { transaction ->
                                    parseTransaction(transaction, author, v, language, mappings, saveToDatabase, kmehrIndex).services
                                } ?: setOf()
                            }?.toSet()
                    } ?: setOf()),
            openingDate = contactDate,
            closingDate = trn.isIscomplete.let { if (it) contactDate else null },
            subContacts = simplifiedSubContacts
        )
    }

    /**
     * Extracts all the utis from a LnkType.
     * @param lnk the LnkType.
     * @return a pair containing the uti type and the set of utis.
     */
    private fun extractUtis(lnk: LnkType): Pair<String, Set<String>> {
        val utis = lnk.mediatype?.value()?.let {
            UTI.utisForMimeType(it).toList()
        } ?: listOf(SimpleUTIDetector().detectUTI(lnk.value.inputStream(), null, null))

        return (utis.firstOrNull()?.identifier ?: "com.adobe.pdf").let {
            val otherUtis = (if (utis.size > 1) utis.subList(1, utis.size).map { u -> u.identifier } else listOf()).toSet()
            if (it == "public.plain-text") {
                Pair("public.plainText", otherUtis + "public.plain-text")
            } else {
                Pair(it, otherUtis)
            }
        }
    }

    /**
     * Converts a TransactionType to an iCure Contact. It parses all the ItemTypes the transaction has as Services,
     * SubContacts, and HealthElements.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ImportResult.
     * @param language the language of the Content of Services and HealthElements.
     * @param mappings
     * @param saveToDatabase whether to save the new entities to the database.
     * @param kmehrIndex the KmehrMessageIndex
     * @return an iCure Contact.
     */
    private suspend fun parseGenericTransaction(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex,
    ): Contact {
        val contactDate = extractTransactionDateTime(trn)
        val trnAuthorHcpId = extractTransactionAuthor(trn, saveToDatabase, author, v)

        val transactionMfid = getTransactionMFID(trn)

        val trnCd = trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value
        val trnTypeCd = trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION_TYPE }?.value

        val contactId = transactionMfid?.let { kmehrIndex.transactionIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString()
        val trnItems = trn.findItems()
        val formId = kmehrIndex.formIdMask.xor(UUID.fromString(contactId)).toString()

        val (services, subContacts) = trnItems.filter { item ->
            val cdItem = item.cds.find { it.s == CDITEMschemes.CD_ITEM }?.value ?: "note"
            if (cdItem == "healthcareelement") {
                trnItems.none { checkItem ->
                    val checkCdItem = checkItem.cds.find { it.s == CDITEMschemes.CD_ITEM }?.value ?: "note"
                    checkCdItem != "healthcareelement" && heItemTypes.contains(checkCdItem) && isHealthElementTypeEqual(item, checkItem)
                } // Get rid of duplicates
            } else {
                true
            }
        }.fold(Pair(listOf<Service>(), listOf<SubContact>())) { (svcs, sbctcs), item ->
            val (label, tags) = mapItem(item, mappings, language)
            when (val cdItem = tags.find { it.type == "CD-ITEM" }?.code ?: "note") {
                in heItemTypes -> {
                    parseAndLinkHealthcareElement(cdItem, label, item, author, trnAuthorHcpId, v, contactId, saveToDatabase, kmehrIndex, null, language, mappings)
                    Pair(svcs, sbctcs)
                }
                "encountertype", "encounterdatetime", "encounterlocation" -> Pair(svcs, sbctcs) // already added at contact level
                "insurancystatus", "gmdmanager", "healthcareapproach" -> Pair(svcs, sbctcs) // not services,
                "incapacity" -> parseIncapacity(item, author, trnAuthorHcpId, language, kmehrIndex, contactId, formId, transactionMfid).let {
                    val (services, subcontacts) = it
                    Pair(svcs + services, sbctcs + subcontacts)
                }
                else -> {
                    val mfId = getItemMFID(item)
                    val service = parseGenericItem(cdItem, label, item, author, trnAuthorHcpId, language, kmehrIndex).let { service ->
                        service.copy(tags = service.tags + tags.filter { it.type != "CD-ITEM" })
                    }.let { service ->
                        if (cdItem == "diagnostic") {
                            // diagnostics are in MSOAP form but also create an HealthcareElement
                            parseAndLinkHealthcareElement(cdItem, label, item, author, trnAuthorHcpId, v, contactId, saveToDatabase, kmehrIndex, service, language, mappings)
                        }
                        when {
                            setOf("vaccine", "acts").contains(cdItem) -> service.copy(label = "Actes")
                            isMedication(service) -> service.copy(label = "Medication")
                            else -> service
                        }
                    }

                    Pair(svcs + service, makeSubContact(contactId, formId, mfId, service, kmehrIndex)?.let { sbctcs + it } ?: sbctcs)
                }
            }
        }

        val simplifiedSubContacts = simplifySubContacts(subContacts).toSet()
        if (simplifiedSubContacts.isNotEmpty()) {
            v.forms.addAll(
                simplifiedSubContacts
                    .filter { sc -> !v.forms.any { it.id == sc.formId } && sc.services.isNotEmpty() }
                    .map { it.formId ?: idGenerator.newGUID().toString() }
                    .toSet()
                    .map {
                        Form(
                            id = it,
                            parent = if (it == formId) kmehrIndex.transactionChildOf[transactionMfid]?.firstOrNull()?.let { kmehrIndex.transactionIds[it]?.first?.let { cid -> kmehrIndex.formIdMask.xor(cid).toString() } } else null,
                            contactId = contactId,
                            author = author.id,
                            responsible = trnAuthorHcpId,
                            created = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                            modified = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                        )
                    }
            )
        } else {
            v.forms.add(
                Form(
                    id = formId,
                    contactId = contactId,
                    author = author.id,
                    responsible = trnAuthorHcpId,
                    created = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                    modified = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                ),
            )
        }

        return Contact(
            id = contactId,
            author = author.id,
            responsible = trnAuthorHcpId,
            created = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            modified = trn.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            openingDate = contactDate,
            closingDate = trn.isIscomplete.let { if (it) contactDate else null },
            tags = listOfNotNull(trnCd, trnTypeCd).map { CodeStub.from("CD-TRANSACTION", it, "1.0") }.toSet(),
            descr = trn.headingsAndItemsAndTexts.filterIsInstance<TextType>().firstOrNull()?.value,
            location =
                trn.findItem { it -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encounterlocation" } }
                    ?.let {
                        it.contents?.flatMap { c -> c.texts.map { t -> t.value } }?.joinToString(",")
                    },
            encounterType = trn.findItem { it -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encountertype" } }
                ?.let {
                    it.contents?.firstNotNullOfOrNull { content ->
                        content.cds?.find { cd -> cd.s == CDCONTENTschemes.CD_ENCOUNTER }?.let { cdContent ->
                            CodeStub.from("CD-ENCOUNTER", cdContent.value, "1.0")
                        }
                    }
                } ?: CodeStub.from("CD-ENCOUNTER", "consultation", "1.0"),
            services = services.toSet() +
                    (transactionMfid?.let {
                        kmehrIndex.parentOf[it]?.flatMap { kmehrId ->
                            kmehrIndex.transactionIds[kmehrId]?.second?.let { transactionType ->
                                parseTransaction(transactionType, author, v, language, mappings, saveToDatabase, kmehrIndex).services } ?: setOf() }?.toSet() } ?: setOf()),
            subContacts = simplifiedSubContacts
        )
    }

    /**
     * Extracts the CD-ITEM CodeStub from the current ItemType and, if possible, maps it using the map passed as
     * parameter.
     * Finally, it finds the label for the language passed as parameter.
     * @param item the ItemType.
     * @param mappings a map where each key is a CD-ITEM value and each value a list of ImportMapping.
     * @param language the language of the label.
     * @return a Pair where the key is the label and the value a set of CD-ITEMs CodeStubs.
     */
    private fun mapItem(item: ItemType, mappings: Map<String, List<ImportMapping>>, language: String): Pair<String, Set<CodeStub>> {
        val guessedCdItem = (item.cds.find { it.s == CDITEMschemes.CD_ITEM }?.value ?: "note").let { cdItem ->
            when {
                item.contents.any { it.cds.any { cds -> cds.s == CDCONTENTschemes.LOCAL && cds.sl == "MEDINOTE.MEDICALCODEID" } } -> "acts"
                else -> cdItem
            }
        }
        val mapping = mappings[guessedCdItem]?.find {
            ((it.lifecycle ?: "*") == "*" || it.lifecycle == item.lifecycle?.cd?.value?.value()) &&
                (((it.content ?: "*") == "*") || item.hasContentOfType(it.content)) &&
                (((it.cdLocal ?: "*") == "*") || (it.cdLocal?.split("|")
                    ?.let { (cdl, cdlcode) ->
                        item.cds.any { cds -> cds.s == CDITEMschemes.LOCAL && cds.sl == cdl && cds.value == cdlcode }
                    } != false))
        }

        val cdItem = mapping?.tags?.find { it.type == "CD-ITEM" }?.code ?: guessedCdItem
        val label = item.cds.find { it.s == CDITEMschemes.LOCAL && it.sl == "org.taktik.icure.label" }?.value
            ?: mapping?.label?.get(language)
            ?: item.contents.filter { (it.texts?.size ?: 0) > 0 }
                .flatMap {
                    it.texts.filter { textType ->
                        textType.l == language
                    }.map { textType ->
                        textType.value
                    }
                }.takeIf { it.isNotEmpty() }
                ?.joinToString(" ")
            ?: mappings["note"]?.lastOrNull()?.label?.get(language)
            ?: "Note"
        return label to (setOf(CodeStub.from("CD-ITEM", cdItem, "1")) + (mapping?.tags?.filter { it.type != "CD-ITEM" } ?: setOf()))
    }

    /**
     * Checks if two ItemTypes represent the same HealthElement based on the equality of their record date time,
     * begin moment, life cycle, tags, codes and item description.
     * @param item the first ItemType to compare.
     * @param checkItem the second ItemType to compare.
     * @return true if the two ItemTypes represent the same HealthElement, false otherwise.
     */
    private fun isHealthElementTypeEqual(item: ItemType, checkItem: ItemType) =
        item.recorddatetime == checkItem.recorddatetime &&
            item.beginmoment == checkItem.beginmoment &&
            item.lifecycle == checkItem.lifecycle &&
            extractTags(item) == extractTags(checkItem) &&
            extractCodes(item) == extractCodes(checkItem) &&
            getItemDescription(item, "") == getItemDescription(checkItem, "")

    /**
     * Creates a new PlanOfAction based on the ItemType passed as parameter.
     * @param cdItem the code to add to the PlanOfAction.
     * @param label the label of the description contained in the ItemType that will be added to the PlanOfAction.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param trnAuthorHcpId the id of the HCP responsible for the import.
     * @return a PlanOfAction.
     */
    private fun parseHealthcareApproach(cdItem: String = "healthcareapproach", label: String, item: ItemType, author: User, trnAuthorHcpId: String): PlanOfAction {
        val poaDate = item.beginmoment?.let { Utils.makeFuzzyLongFromMomentType(it) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) }
            ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)
        return PlanOfAction(
            id = idGenerator.newGUID().toString(),
            descr = getItemDescription(item, label),
            author = author.id,
            responsible = trnAuthorHcpId,
            tags = setOf(CodeStub.from("CD-ITEM", cdItem, "1")) + extractTags(item) + (
                item.lifecycle?.let { listOf(CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1")) }
                    ?: listOf()
                ),
            codes = extractCodes(item),
            valueDate = poaDate,
            openingDate = poaDate,
            closingDate = item.endmoment?.let { Utils.makeFuzzyLongFromMomentType(it) },
            created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
        )
    }


    /**
     * Convert the first non-null insurance in the ItemType passed as parameter to a list of iCure Insurabilities.
     * @param item the ItemType.
     * @return A List of Insurabilities.
      */
    private suspend fun parseInsurancyStatus(item: ItemType): List<Insurability> =
        item.contents.find { it.insurance != null }?.insurance?.let {
            listOf(
                Insurability(
                    insuranceId = if (it.id.s == IDINSURANCEschemes.ID_INSURANCE) insuranceLogic.listInsurancesByCode(it.id.value).firstOrNull()?.id else null,
                    parameters = mapOf(
                        "tc1" to it.cg1,
                        "tc2" to it.cg2,
                    ),
                    identificationNumber = it.membership,
                ),
            )
        } ?: listOf()

    /**
     * Checks if a Service is a Medication Service.
     * @param service the Service to check.
     * @return true if the Service has at least one content with a non-null medication value, false otherwise.
     */
    private fun isMedication(service: Service): Boolean =
        service.content.values.any { it.medicationValue != null }

    /**
     * Creates a set of Services and SubContacts related to the ItemTypes for the Incapacities present in the SMF.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param trnAuthorHcpId the id of the HCP responsible for the import.
     * @param language the language of the content of Contacts and SubContacts.
     * @param kmehrIndex the KmehrMessageIndex.
     * @param contactId the id of the main Contact.
     * @param formId the id of the Form to create.
     * @param transactionMfid the MF-ID of the SMF.
     * @return a triple containing the Services, the SubContacts and the Form.
     */
    private suspend fun parseIncapacity(
        item: ItemType,
        author: User,
        trnAuthorHcpId: String,
        language: String,
        kmehrIndex: KmehrMessageIndex,
        contactId: String,
        formId: String,
        transactionMfid: String?,
    ): Triple<List<Service>, Collection<SubContact>, Form> {
        val mfId = getItemMFID(item)
        val ittform = Form(
            id = formId,
            formTemplateId = getFormTemplateIdByGuid(author, "FFFFFFFF-FFFF-FFFF-FFFF-INCAPACITY00"), // ITT form template
            parent = transactionMfid?.let { tmfId ->
                kmehrIndex.transactionChildOf[tmfId]
                    ?.firstOrNull()
                    ?.let {
                        kmehrIndex.transactionIds[it]?.first?.let { cid -> kmehrIndex.formIdMask.xor(cid).toString() }
                    }
            },
            contactId = contactId,
            responsible = trnAuthorHcpId,
            author = author.id,
            codes = extractCodes(item).toMutableSet(),
            created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            tags = item.lifecycle?.let { setOf(CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1")) } ?: setOf(),
            descr = "6FF898B0-2694-4973-83F3-1F93C6DADC61", // Magic number
        )

        val mapServ = mapOf(
            "incapacité de" to
                item.contents.find { it.incapacity != null }?.let { content ->
                    // TODO Dorian fix that
                    content.incapacity.cds.filterIsInstance<CDINCAPACITY>().map { it.value }
                }?.let {
                    Pair(
                        Content(stringValue = it.joinToString("|") { incapacityValue -> incapacityValue.value() }),
                        it.map { code -> CodeStub.from("CD-INCAPACITY", code.value(), "1") },
                    )
                },
            "du" to item.beginmoment?.let { Content(fuzzyDateValue = Utils.makeFuzzyLongFromMomentType(it)) },
            "au" to item.endmoment?.let { Content(fuzzyDateValue = Utils.makeFuzzyLongFromMomentType(it)) },
            "inclus/exclus" to Content(stringValue = "inclus"), // no kmehr equivalent
            "pour cause de" to
                item.contents.find { it.incapacity != null }?.let {
                    // TODO Dorian fix that
                    it.incapacity.incapacityreason?.cd?.value
                }?.let {
                    Pair(
                        Content(stringValue = it.value()),
                        listOf(CodeStub.from("CD-INCAPACITYREASON", it.value(), "1")),
                    )
                },
            "Commentaire" to Content(stringValue = item.texts.joinToString(" ") { it.value }),
            // missing:
            // "Accident suvenu le"
            // "Sortie"
            // "autres"
            // "reprise d'activité partielle"
            // "pourcentage"
            // "totale"
        )

        val mainServiceId = mfId?.let { kmehrIndex.itemIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString()
        val servicesAndSubContacts = mapServ.map { entry ->
            entry.value?.let {
                val service = Service(
                    id = mainServiceId,
                    label = entry.key,
                    contactId = contactId,
                    responsible = trnAuthorHcpId,
                    index = 1L,
                    author = author.id,
                    qualifiedLinks = mapOf(LinkQualification.relatedService to mapOf(UUID.randomUUID().toString() to mainServiceId)),
                    created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                    modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                    valueDate = item.beginmoment?.let { Utils.makeFuzzyLongFromMomentType(it) },
                    content = (it as? Pair<Content, List<CodeStub>>)?.let { content -> mapOf(language to content.first) }
                        ?: (it as? Content)?.let { content -> mapOf(language to content) } ?: mapOf(),
                    tags = setOf(CodeStub.from("CD-ITEM", "incapacity", "1")) + (
                        (it as? Pair<Content, List<CodeStub>>)?.second?.toSet()
                            ?: setOf()
                        ),
                )
                service to makeSubContact(contactId, ittform.id, mfId, service, kmehrIndex)
            }
        }.filterNotNull()
        return Triple(servicesAndSubContacts.map { it.first }, simplifySubContacts(servicesAndSubContacts.flatMap { it.second!! }), ittform)
    }

    /**
     * Creates a new HealthElement using the information contained in an ItemType.
     * @param cdItem the code of the tag to be added to the HealthElement.
     * @param label the label to use if the ItemType contains no description.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param trnAuthorHcpId the id of the HCP responsible for the input.
     * @param contactId the id of the opening contact of the HealthElement.
     * @param kmehrIndex the KmehrMessageIndex.
     * @param linkedService the Service linked to this HealthElement.
     * @param language the language to pass to the MapItem function.
     * @param mappings the mappings between ItemTypes and ImportMappings.
     * @return a HealthElement.
     */
    private fun parseHealthcareElement(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        trnAuthorHcpId: String,
        contactId: String,
        kmehrIndex: KmehrMessageIndex,
        linkedService: Service? = null,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
    ): HealthElement {
        // this method is used for comparison so should not have side effects
        val heDate = extractValueDate(item)
        val mfId = getItemMFID(item)

        val tags = setOfNotNull(
            item.certainty?.let { CodeStub.from("CD-CERTAINTY", it.cd.value.value(), "1") },
            item.severity?.let { CodeStub.from("CD-SEVERITY", it.cd.value.value(), "1") },
            item.lifecycle?.let { CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1") }
        )

        return HealthElement(
            id = idGenerator.newGUID().toString(),
            healthElementId = mfId?.let { kmehrIndex.itemIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString(),
            descr = getItemDescription(item, label),
            idService = linkedService?.id,
            tags = tags + setOf(CodeStub.from("CD-ITEM", cdItem, "1")) + extractTags(item),
            author = author.id,
            responsible = trnAuthorHcpId,
            codes = extractCodes(item),
            valueDate = heDate,
            openingDate = heDate,
            closingDate = item.endmoment?.let { Utils.makeFuzzyLongFromMomentType(it) },
            idOpeningContact = contactId,
            created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            status = extractStatus(item),
            relevant = item.isIsrelevant ?: false,
            plansOfAction = kmehrIndex.approachFor.filter { (_, v) -> v.contains(mfId) }.mapNotNull { (k, _) ->
                kmehrIndex.itemIds[k]?.second?.let { item ->
                    val (l, _) = mapItem(item, mappings, language)
                    parseHealthcareApproach(label = l, item = item, author = author, trnAuthorHcpId = trnAuthorHcpId)
                }
            }.toList()
        )
    }

    /**
     * Extracts the status of a given item based on its lifecycle and relevance.
     * @param item the ItemType to extract the status from.
     * @return an Int representing the status of the ItemType.
     */
    private fun extractStatus(item: ItemType) =
        (item.lifecycle?.cd?.value?.value()
            ?.let {
                when (it) {
                    "inactive", "aborted", "canceled" -> 1
                    "notpresent", "excluded" -> 4
                    else -> 0
                }
            } ?: 0) + if (item.isIsrelevant != true) 2 else 0

    /**
     * Extracts the date of the ItemType passed as parameter and converts it to a FuzzyDate. The method uses the
     * ItemType begin moment. If that is null uses the record date time and finally falls back to the current date.
     * @param item the ItemType.
     * @return a fuzzy date.
     */
    private fun extractValueDate(item: ItemType) =
        item.beginmoment?.let { Utils.makeFuzzyLongFromMomentType(it) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) }
            ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)

    /**
     * Creates a new HealthElement using the information contained in an ItemType, saves it to the db, if specified, and
     * adds it to the ImportResult passed as parameter.
     * @param cdItem the code of the tag to be added to the HealthElement.
     * @param label the label to use if the ItemType contains no description.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param trnAuthorHcpId the id of the HCP responsible for the input.
     * @param contactId the id of the opening contact of the HealthElement.
     * @param v the ImportResult where the HealthElement will be added.
     * @param saveToDatabase whether to save the new HealthElement to the db.
     * @param kmehrIndex the KmehrMessageIndex.
     * @param linkedService the Service linked to this HealthElement.
     * @param language the language to pass to the MapItem function.
     * @param mappings the mappings between ItemTypes and ImportMappings.
     * @return a HealthElement.
     */
    private suspend fun parseAndLinkHealthcareElement(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        trnAuthorHcpId: String,
        v: ImportResult,
        contactId: String,
        saveToDatabase: Boolean,
        kmehrIndex: KmehrMessageIndex,
        linkedService: Service? = null,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
    ): HealthElement? =
        parseHealthcareElement(cdItem, label, item, author, trnAuthorHcpId, contactId, kmehrIndex, linkedService, language, mappings).let { he ->
            (if (saveToDatabase) healthElementLogic.createEntities(listOf(he)).firstOrNull() else he)?.also {
                v.hes.add(it)
            }
        }

    /**
     * Extract all the codes from an ItemTypes and returns them as a Set of CodeStub.
     * @param item The ItemType.
     * @return A Set of CodeStub.
     */
    private fun extractCodes(item: ItemType): Set<CodeStub> =
        (item.cds
            .filter { it.s == CDITEMschemes.ICPC || it.s == CDITEMschemes.ICD }
            .map { CodeStub.from(it.s.value(), it.value, it.sv) } +
        item.contents
            .filter { (it.cds?.size ?: 0) > 0 }
            .flatMap { contentType ->
                contentType.cds.filter {
                    listOf(
                        CDCONTENTschemes.CD_DRUG_CNK,
                        CDCONTENTschemes.ICD,
                        CDCONTENTschemes.ICPC,
                        CDCONTENTschemes.CD_ATC,
                        CDCONTENTschemes.CD_PATIENTWILL,
                        CDCONTENTschemes.CD_VACCINEINDICATION,
                    ).contains(it.s)
                }.map { CodeStub.from(it.s.value(), it.value, it.sv) } + contentType.cds.filter {
                    (it.s == CDCONTENTschemes.LOCAL && it.sl == "BE-THESAURUS-PROCEDURES")
                }.map { CodeStub.from(it.sl, it.value, it.sv) } + contentType.cds.filter {
                    (it.s == CDCONTENTschemes.CD_CLINICAL)
                }.map { CodeStub.from("BE-THESAURUS", it.value, it.sv) } + contentType.cds.filter {
                    (it.s == CDCONTENTschemes.LOCAL && it.sl.startsWith("MS-EXTRADATA"))
                }.map { CodeStub.from(it.sl, it.value, it.sv) }
            }
        ).toSet()

    /**
     * Extracts all the tags from the CD_PARAMETER, CD_LAB, CD_TECHNICAL, CD_CONTACT_PERSON, and LOCAL CD-ITEM schemes
     * of an ItemType and returns them as a Collection of CodeStubs.
     * @param item The ItemType.
     * @return A Collection of CodeStubs.
     */
    private fun extractTags(item: ItemType): Collection<CodeStub> =
        (item.cds
            .filter { it.s == CDITEMschemes.CD_PARAMETER || it.s == CDITEMschemes.CD_LAB || it.s == CDITEMschemes.CD_TECHNICAL || it.s == CDITEMschemes.CD_CONTACT_PERSON }
            .map { CodeStub.from(it.s.value(), it.value, it.sv) } +
        item.cds
            .filter { (it.s == CDITEMschemes.LOCAL && it.sl.equals("LOCAL-PARAMETER")) }
            .map { CodeStub.from(it.sl, it.dn ?: it.value, it.sv) } +
        item.cds
            .filter { (it.s == CDITEMschemes.LOCAL && it.sl.equals("GPSMF-PARAMETER")) }
            .map { CodeStub.from(it.sl, it.dn ?: it.value, it.sv) } +
        item.contents
            .filter { (it.cds?.size ?: 0) > 0 }
            .flatMap { contentType ->
                contentType.cds.filter {
                        listOf(CDCONTENTschemes.CD_LAB).contains(it.s)
                    }.map { CodeStub.from(it.s.value(), it.value, it.sv) }
            }
        ).toSet()

    /**
     * Converts a KmEHR ItemType to a Service which id depends on the MF-ID of the SMF. It adds as content the data
     * about medication and prescriptions.
     * @param cdItem the codes to add.
     * @param label the label of the Service.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param trnAuthorHcpId the id of the HCP responsible for the import.
     * @param language the language of the Service content.
     * @param kmehrIndex the KmehrMessageIndex.
     * @return an iCure Service.
     */
    private fun parseGenericItem(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        trnAuthorHcpId: String,
        language: String,
        kmehrIndex: KmehrMessageIndex,
    ): Service {
        val serviceDate = item.beginmoment?.let { Utils.makeFuzzyLongFromMomentType(it) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) }
            ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)
        val tags = setOf(CodeStub.from("CD-ITEM", cdItem, "1")) + extractTags(item) + (
            item.temporality?.cd?.value?.let { setOf(CodeStub.from("CD-TEMPORALITY", it.value(), "1")) }
                ?: setOf()
            ) + (
            item.lifecycle?.let { setOf(CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1")) }
                ?: setOf()
            )
        val mfId = getItemMFID(item)

        return Service(
            id = mfId?.let { kmehrIndex.itemIds[it]?.first?.toString() } ?: idGenerator.newGUID().toString(),
            label = tags.find { it.type == "CD-PARAMETER" }?.let {
                consultationFormMeasureLabels[it.code]
            } ?: label,
            codes = extractCodes(item),
            tags = tags,
            responsible = trnAuthorHcpId,
            author = author.id,
            valueDate = serviceDate,
            openingDate = serviceDate,
            closingDate = item.endmoment?.let { Utils.makeFuzzyLongFromMomentType(it) },
            created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            qualifiedLinks = mfId?.let {
                kmehrIndex.attestationOf[it]
                    ?.firstOrNull()
                    ?.let { itemId ->
                        kmehrIndex.itemIds[itemId]?.first?.toString()
                            ?.let { itt -> mapOf(LinkQualification.relatedService to mapOf(UUID.randomUUID().toString() to itt)) }
                    }
            } ?: mapOf(),
            status = (item.lifecycle?.cd?.value?.value()
                    ?.let {
                        when (it) {
                            "inactive", "aborted", "canceled" -> 1
                            "notpresent", "excluded" -> 4
                            else -> 0
                        }
                    } ?: 0) + if (item.isIsrelevant != true) 2 else 0,
            content = when {
                (item.contents.any { it.substanceproduct != null || it.medicinalproduct != null || it.compoundprescription != null }) -> {
                    Content(
                        medicationValue = Medication(
                            substanceProduct = item.contents.firstOrNull { it.substanceproduct != null }
                                ?.let { contentType ->
                                contentType.substanceproduct?.let {
                                    Substanceproduct(
                                        intendedcds = it.intendedcd?.let { cd -> listOf(CodeStub.from(cd.s.value(), cd.value, cd.sv)) }
                                            ?: listOf(),
                                        intendedname = it.intendedname.toString(),
                                    )
                                }
                            },
                            medicinalProduct = item.contents.firstOrNull { it.medicinalproduct != null }?.let { contentType ->
                                contentType.medicinalproduct?.let {
                                    Medicinalproduct(
                                        intendedcds = it.intendedcds?.map { cd -> CodeStub.from(cd.s.value(), cd.value, cd.sv) }
                                            ?: listOf(),
                                        intendedname = it.intendedname.toString(),
                                    )
                                }
                            },
                            compoundPrescription = item.contents.firstOrNull {
                                it.compoundprescription?.content?.isNotEmpty() ?: false
                            }?.let { contentType ->
                                // spec is unclear, some software put text in <magistraltext> some put it directly in compoundprescription
                                // try to detect each case
                                contentType.compoundprescription?.content?.mapNotNull {
                                    // spec is unclear, some software put text in <magistraltext> some put it directly in compoundprescription
                                    // try to detect each case
                                    if (it is String) {
                                        it
                                    } else {
                                        if (it is TextType) {
                                            it.value
                                        } else {
                                            try {
                                                if ((it as JAXBElement<*>).value is TextType) {
                                                    (it.value as TextType).value
                                                } else {
                                                    null
                                                }
                                            } catch (ex: Exception) {
                                                null
                                            }
                                        }
                                    }
                                }?.joinToString(" ") { it.trim() }
                            } ?: "",
                            instructionForPatient = (
                                listOf(item.instructionforpatient?.value) +
                                    item.lnks.mapNotNull { it.value?.toString(Charsets.UTF_8) }
                                ).filterNotNull().joinToString(", ").ifBlank { null },
                            posology = item.posology?.text?.value, // posology can be complex but SMF spec recommends text type
                            duration = item.duration?.let { dt ->
                                Duration(
                                    value = dt.decimal.toDouble(),
                                    unit = dt.unit?.cd?.let { CodeStub.from(it.s.value(), it.value, it.sv) },
                                )
                            },
                            numberOfPackages = item.quantity?.decimal?.toInt(),
                            batch = item.batch,
                            beginMoment = item.beginmoment?.let { Utils.makeFuzzyLongFromMomentType(it) },
                            endMoment = item.endmoment?.let { Utils.makeFuzzyLongFromMomentType(it) },
                        ),
                    )
                }
                (item.contents.any { it.decimal != null }) -> item.contents.firstOrNull { it.decimal != null }?.let {
                    val comment = getItemDescription(item, "")
                    if (it.unit != null) {
                        Content(measureValue = Measure(value = it.decimal.toDouble(), unit = it.unit?.cd?.value))
                    } else {
                        Content(measureValue = Measure(value = it.decimal.toDouble(), comment = comment))
                    }
                }
                (item.contents.any { it.texts.any { t -> t.value?.isNotBlank() ?: false } }) -> {
                    val textValue = item.contents
                        .filter { (it.texts?.size ?: 0) > 0 }
                        .flatMap { it.texts.map { t -> t.value } }
                        .joinToString(", ")
                        .takeIf { it.isNotBlank() }
                    val measureValue = if (cdItem == "parameter") {
                        // Try harder to convert to measure
                        item.contents.filter { (it.texts?.size ?: 0) > 0 }.flatMap { contentType ->
                            contentType.texts.map { textType ->
                                textType.value?.let {
                                    val unit = it.replace(Regex("[0-9.,] *"), "")
                                    val value = it.replace(Regex("([0-9.,]) *.*"), "$1")

                                    try {
                                        Measure(
                                            value = value.toDouble(),
                                            unit = unit,
                                        )
                                    } catch (ignored: NumberFormatException) {
                                        null
                                    }
                                }
                            }
                        }.filterNotNull().firstOrNull()
                    } else {
                        null
                    }
                    if (measureValue == null) {
                        Content(stringValue = textValue)
                    } else {
                        Content(measureValue = measureValue)
                    }
                }
                (item.contents.any { it.isBoolean != null }) -> item.contents.firstOrNull { it.isBoolean != null }?.let {
                    Content(booleanValue = it.isBoolean)
                }
                else -> null
            }?.let { mapOf(language to it) } ?: mapOf(),
        )
    }

    /**
     * Retrieves the description of the given [item].
     * The description is composed of the concatenated values of the value field of all TextType elements
     * and of all the value field of all TextType elements inside each Content element of the given [item].
     * If the resulting description is empty, returns the [defaultValue] instead.
     * @param item the item to extract the description from.
     * @param defaultValue the default value to return if no description can be found.
     * @return the description of the item, or the default value if no description can be found.
     */
    private fun getItemDescription(item: ItemType, defaultValue: String): String =
        (item.texts.map { it.value } + item.contents.flatMap { it.texts.map { t -> t.value } })
            .filter { it != null && it.trim() != "" }
            .joinToString(", ")
            .takeIf { it.trim() == "" } ?: defaultValue

    /**
     * Checks if an ItemType has a Content of the type passed as parameter.
     * @param content the Content
     * @return true if the ItemType has the content, false otherwise
     */
    private fun ItemType.hasContentOfType(content: String?): Boolean =
        content?.let { c ->
            c == "m" && this.contents.any { it.medicinalproduct != null || it.substanceproduct != null || it.compoundprescription != null } ||
                    c == "s" && this.contents.any { (it.texts?.size ?: 0) > 0 || (it.cds?.size ?: 0) > 0 || it.hcparty != null }
        } ?: true

    /**
     * Given a HcpartyType, tries to retrieve the corresponding iCure HealthcareParty by Nihii, by Niss and ultimately
     * by name and adds it to the ImportResult. If the HCP does not exist, it creates it and saves it to the db.
     * @param p The HcpartyType
     * @return a HCP
     */
    private suspend fun createOrProcessHcp(p: HcpartyType, saveToDatabase: Boolean, v: ImportResult): HealthcareParty? {
        val nihii = validNihiiOrNull(p.ids.find { it.s == IDHCPARTYschemes.ID_HCPARTY }?.value)
        val niss = validSsinOrNull(p.ids.find { it.s == IDHCPARTYschemes.INSS }?.value)
        val specialty: String? = p.cds.find { it.s == CDHCPARTYschemes.CD_HCPARTY }?.value?.trim()

        // test if already exist in current file
        val hcpBySsinOrNihii = v.hcps.find {
            nihii?.let { ni -> it.nihii == ni } == true ||
                niss?.let { ni -> it.ssin == ni } == true ||
                (
                    ((nihii == null || nihii.trim() == "") && (niss == null || niss.trim() == "")) &&
                        it.firstName?.trim() == p.firstname?.trim() &&
                        it.lastName?.trim() == p.familyname?.trim() &&
                        it.name?.trim() == p.name?.trim() &&
                        it.speciality == specialty
                    )
        }  // test if already exist in db
        ?: nihii?.let { healthcarePartyLogic.listHealthcarePartiesByNihii(it).firstOrNull() }?.also { v.hcps.add(it) }
        ?: niss?.let { healthcarePartyLogic.listHealthcarePartiesBySsin(it).firstOrNull() }?.also { v.hcps.add(it) }

        val existing = if (hcpBySsinOrNihii == null && ((nihii == null || nihii.trim() == "") && (niss == null || niss.trim() == "")) &&
            p.firstname?.trim()?.let { it == "" } != false &&
            p.familyname?.trim()?.let { it == "" } != false
        ) {
            p.name
                ?.let { healthcarePartyLogic.listHealthcarePartiesByName(p.name).firstOrNull() }
                ?.also {
                    v.hcps.add(it) // do not create it, but should appear in patient external hcparties
                }
        } else hcpBySsinOrNihii

        return existing
            ?: try {
                    copyFromHcpToHcp(p, HealthcareParty(id = idGenerator.newGUID().toString(), nihii = nihii, ssin = niss)).also {
                        v.hcps.add(it)
                        if (saveToDatabase) healthcarePartyLogic.createHealthcareParty(it)
                    }
                } catch (e: MissingRequirementsException) {
                    null
                }
    }

    /**
     * Creates a new HCP from a HcpartyType, adding information from an external HCP.
     * @param p The HcpartyType.
     * @param hcp The external HCP.
     * @return A HealthacareParty.
     */
    private fun copyFromHcpToHcp(p: HcpartyType, hcp: HealthcareParty): HealthcareParty =
        hcp.copy(
            firstName = hcp.firstName ?: p.firstname,
            lastName = hcp.lastName ?: p.familyname,
            name = hcp.name ?: p.name,
            ssin = hcp.ssin ?: p.ids.find { it.s == IDHCPARTYschemes.INSS }?.value,
            nihii = hcp.nihii ?: p.ids.find { it.s == IDHCPARTYschemes.ID_HCPARTY }?.value,
            speciality = hcp.speciality ?: p.cds.find { it.s == CDHCPARTYschemes.CD_HCPARTY }?.value,
            addresses = hcp.addresses + (
                p.addresses?.let { addresses ->
                    addresses.map { address ->
                        val addressType = address.cds.find { it.s == CDADDRESSschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) }
                        Address(
                            addressType = addressType,
                            street = address.street,
                            city = address.city,
                            houseNumber = address.housenumber,
                            postboxNumber = address.postboxnumber,
                            postalCode = address.zip,
                            country = address.country?.cd?.value,
                            telecoms = p.telecoms
                                .filter { t -> t.cds.find { it.s == CDTELECOMschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) } == addressType }
                                .mapNotNull { t ->
                                    t.cds.find { it.s == CDTELECOMschemes.CD_TELECOM }?.let { TelecomType.valueOf(it.value) }?.let { telecomType ->
                                        Telecom(telecomType = telecomType, telecomNumber = t.telecomnumber)
                                    }
                                }
                        )
                    }
                } ?: listOf()
            )
        )

    /**
     * Checks if a Patient exists by ssin, date of birth, or name for the full HCP hierarchy of the author.
     * @param p the PersonType corresponding to the Patient.
     * @param author the User responsible for the check.
     * @param v the ImportResult.
     * @param dest if not null, this Patient is used.
     * @return a CheckSMFPatientResult instance.
     */
    private suspend fun checkIfPatientExists(
        p: PersonType,
        author: User,
        v: ImportResult,
        dest: Patient? = null,
    ): CheckSMFPatientResult {
        val res = CheckSMFPatientResult()
        val niss = validSsinOrNull(p.ids.find { it.s == IDPATIENTschemes.ID_PATIENT }?.value)
        v.notNull(niss, "Niss shouldn't be null for patient $p")
        res.ssin = niss ?: ""
        res.dateOfBirth = Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date)
        res.firstName = p.firstnames.first()
        res.lastName = p.familyname

        val dbPatient = getExistingPatientWithHcpHierarchy(p, author, v, dest)

        res.exists = (dbPatient != null)
        res.existingPatientId = dbPatient?.id
        return res
    }

    /**
     * Gets a patient using an HCP or one of his parents.
     * @param p the Patient data from the SumEHR
     * @param author the HCP User responsible for the request
     * @param v the SumEHR import result
     * @param dest the patient to return
     * @return the Patient passed as parameter (if not null) or the one retrieved (if any).
     */
    private suspend fun getExistingPatientWithHcpHierarchy(
        p: PersonType,
        author: User,
        v: ImportResult,
        dest: Patient? = null,
    ): Patient? {
        if (author.healthcarePartyId == null) {
            return null
        }

        val hcp = healthcarePartyLogic.getHealthcareParty(author.healthcarePartyId!!)
        return hcp?.parentId?.let { hcpParent ->
            userLogic.listUserIdsByHcpartyId(hcpParent).firstOrNull()?.let { parentAuthorId ->
                userLogic.getUser(parentAuthorId)?.let { parentAuthor ->
                    getExistingPatient(p, parentAuthor, v, dest)
                }
            }
        } ?: getExistingPatient(p, author, v, dest)
    }

    /**
     * If the Patient passed as parameter is not null, returns that. Otherwise, uses the data from the SumEHR to
     * retrieve the patient by Ssin, date of birth or fuzzy name.
     * @param p the Patient data from the SumEHR
     * @param author the HCP User responsible for the request
     * @param v the SumEHR import result
     * @param dest the patient to return
     * @return the Patient passed as parameter (if not null) or the one retrieved (if any).
     */
    private suspend fun getExistingPatient(
        p: PersonType,
        author: User,
        v: ImportResult,
        dest: Patient? = null,
    ): Patient? {
        if (author.healthcarePartyId == null) {
            return null
        }

        val niss = validSsinOrNull(p.ids.find { it.s == IDPATIENTschemes.ID_PATIENT }?.value) // searching empty niss return all patients
        v.notNull(niss, "Niss shouldn't be null for patient $p")

        return dest ?: niss?.let {
                patientLogic.listByHcPartyAndSsinIdsOnly(niss, author.healthcarePartyId!!).firstOrNull()
                    ?.let { patientLogic.getPatient(it) }
            } ?: patientLogic.listByHcPartyDateOfBirthIdsOnly(
                Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date)
                    ?: throw IllegalStateException("Person's date of birth is invalid"),
                author.healthcarePartyId!!,
            ).toList()
            .takeIf { it.isNotEmpty() }
            ?.let {
                patientLogic.getPatients(it).filter {
                    p.firstnames.any { fn -> equals(it.firstName, fn) && equals(it.lastName, p.familyname) }
                }.firstOrNull()
            } ?: patientLogic.listByHcPartyNameContainsFuzzyIdsOnly(
                sanitizeString(p.familyname + p.firstnames.first()),
                author.healthcarePartyId!!).toList()
                .takeIf { it.isNotEmpty() }
                ?.let {
                    patientLogic.getPatients(it).filter { patient ->
                        patient.dateOfBirth?.let { dateOfBirth -> dateOfBirth == Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date) }
                            ?: false
                    }.firstOrNull()
                }
    }

    /**
     * Finds a Patient related to a HCP using the information contained in the ImportResult. If the Patient does not
     * exist, it creates it and save it to the database
     * @param p a PersonType contained in the SumEHR message
     * @param author the HCP User responsible for the import
     * @param v the Import Result
     * @param saveToDatabase whether saving the new Patient to the database or not
     * @param dest if not null, this Patient is used instead of the retrieved one
     * @return the retrieved or newly created HCP
     */
    private suspend fun createOrProcessPatient(
        p: PersonType,
        author: User,
        v: ImportResult,
        saveToDatabase: Boolean,
        dest: Patient? = null,
        insurabilities: List<Insurability> = listOf(),
    ) = getExistingPatientWithHcpHierarchy(p, author, v, dest)?.let { it.copy(insurabilities = it.insurabilities + insurabilities) }
        ?: Patient(
            id = idGenerator.newGUID().toString(),
            insurabilities = insurabilities,
            delegations = author.healthcarePartyId?.let { mapOf(it to setOf()) }
                ?: mapOf(),
        ).let {
            copyFromPersonToPatient(p, it)
        }.let { if (saveToDatabase) patientLogic.createPatient(it) else it }

    /**
     * Creates a new patient merging the information from an existing patient and the ones retrieved from the SumEHR.
     * @param p the Patient information retrieved from the SumEHR
     * @param patient the existing Patient
     * @param force true to prefer the data retrieved from the SumEHR
     * @return a Patient
     */
    private fun copyFromPersonToPatient(p: PersonType, patient: Patient, force: Boolean = true): Patient =
        patient.copy(
            firstName = p.firstnames.firstOrNull(),
            lastName = p.familyname,
            dateOfBirth = Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date),
            ssin = patient.ssin ?: p.ids.find { it.s == IDPATIENTschemes.ID_PATIENT }?.value
                ?: p.ids.find { it.s == IDPATIENTschemes.INSS }?.value,
            placeOfBirth = if (force || patient.placeOfBirth == null) p.birthlocation?.getFullAddress() else patient.placeOfBirth,
            dateOfDeath = if (force || patient.dateOfDeath == null) p.deathdate?.let { Utils.makeFuzzyIntFromXMLGregorianCalendar(it.date) } else patient.dateOfDeath,
            placeOfDeath = if (force || patient.placeOfDeath == null) p.deathlocation?.getFullAddress() else patient.placeOfDeath,
            gender = if (force || patient.gender == null) {
                when (p.sex.cd.value) {
                    CDSEXvalues.FEMALE -> Gender.female
                    CDSEXvalues.MALE -> Gender.male
                    CDSEXvalues.UNKNOWN -> Gender.unknown
                    CDSEXvalues.CHANGED -> Gender.changed
                    else -> Gender.unknown
                }
            } else {
                patient.gender
            },
            profession = if (force || patient.profession == null) p.profession?.text?.value else patient.profession,
            externalId = p.ids.firstOrNull { i -> i.s == IDPATIENTschemes.LOCAL && i.sl == "PatientReference" }?.value?.let { patRef ->
                if (force || patient.externalId == null) patRef else patient.externalId
            } ?: patient.externalId,
            alias = p.ids.firstOrNull { i -> i.s == IDPATIENTschemes.LOCAL && i.sl == "PatientAlias" }?.value?.let { alias ->
                if (force || patient.externalId == null) alias else patient.alias
            } ?: patient.alias,
            addresses = patient.addresses + (
                p.addresses?.let { addresses ->
                    addresses.map { address ->
                        val addressType = address.cds.find { it.s == CDADDRESSschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) }
                        Address(
                            addressType = addressType,
                            street = address.street,
                            city = address.city,
                            houseNumber = address.housenumber,
                            postboxNumber = address.postboxnumber,
                            postalCode = address.zip,
                            country = address.country?.cd?.value,
                            telecoms = p.telecoms
                                .filter { t -> t.cds.find { it.s == CDTELECOMschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) } == addressType }
                                .mapNotNull {
                                    it.cds.find { cds -> cds.s == CDTELECOMschemes.CD_TELECOM }
                                        ?.let { cds -> TelecomType.valueOf(cds.value) }
                                        ?.let { telecomType ->
                                            Telecom(telecomType = telecomType, telecomNumber = it.telecomnumber)
                                        }
                                }
                        )
                    }
                } ?: listOf()
            ),
            languages = patient.languages + (
                p.usuallanguage?.let { if (patient.languages.contains(it)) null else listOf(it) }
                    ?: listOf()
                )
        )

    private val consultationFormMeasureLabels: Map<String, String> = mapOf(
        // theses labels are used to identify services associated to form consultation
        // should be lower case
        "weight" to "Poids",
        "height" to "Taille",
        "bmi" to "BMI",
        "heartpulse" to "Pouls",
        // "craneperim" to  "??",
        "hipperim" to "Tour de taille",
        "glycemy" to "Glyc.", // only in form Consultation 09b8db54-84a3-42e7-b8db-5484a352e77f
        "glycemyhba1c" to "HbA1c",
        "pulse" to "R\u00e9gularit\u00e9 du pouls",
        // "apgarscore" to  "??",
        "systolic" to "Tension art\u00e9rielle systolique",
        "diastolic" to "Tension art\u00e9rielle diastolique",
        "temperature" to "T\u00b0",
        // and compound "tension"
    )

    private suspend fun getFormTemplateIdByGuid(author: User, guid: String): String? =
        formTemplateLogic.getFormTemplatesByGuid(author.id, "deptgeneralpractice", guid).firstOrNull()?.id

    data class HeVersionType(val he: HealthElement, val mfId: String, val isANewVersionOfId: String?, var versionId: String?)
    data class DocumentLinkType(val document: Document, val service: Service, val isAChildOfId: String?)
    data class ServiceVersionType(val service: Service, val mfId: String, val isANewVersionOfId: String?, var versionId: String?)
    data class ServiceHeLink(val mfid: String, val serviceId: String, val heMfid: String)

    // internal bookkeeping
    data class InternalState(
        val subcontactLinks: MutableList<ServiceHeLink> = mutableListOf(), // bookkeeping for linking He to Services (map of heId and linked Service/He)
        val heVersionLinks: MutableList<HeVersionType> = mutableListOf(), // bookkeeping for versioning HealthElements
        val heVersionLinksByMFID: MutableMap<String, List<HeVersionType>> = mutableMapOf(),
        val hesByMFID: MutableMap<String, HealthElement> = mutableMapOf(),
        val contactsByMFID: MutableMap<String, Contact> = mutableMapOf(),
        val docLinks: MutableList<Pair<Service, String?>> = mutableListOf(), // services, linked parent contactMFId
        val prescLinks: MutableList<Pair<List<Service>, String?>> = mutableListOf(), // services, linked parent contactMFId
        val approachLinks: MutableList<Triple<PlanOfAction, String?, String?>> = mutableListOf(), // planOfAction, MFId, linked target heMFId
        val formServices: MutableMap<String, Service> = mutableMapOf(), // services to not add to dynamic form because already in a form
        val incapacityForms: MutableList<Form> = mutableListOf(), // to add them to parent consultation form
        val serviceVersionLinks: MutableList<ServiceVersionType> = mutableListOf(), // bookkeeping for versioning services (medications)
        val serviceVersionLinksByMFID: Map<String, List<ServiceVersionType>> = mapOf(),
    )
}

/**
 * Extract the MF-ID from an url using Regex.
 * @param url The url.
 * @return the MF-ID, if any.
 */
fun extractMFIDFromUrl(url: String): String? {
    val regex = Regex("SL=\"MF-ID\"\\sand\\s\\.=\"([^\"]+)\"")
    val result = regex.find(url)
    return result?.groups?.get(1)?.value?.trim()
}

/**
 * Finds the first MF-ID with local scheme in the ItemType passed as parameter.
 * @param item The ItemType.
 * @return the MF-ID, if any.
 */
fun getItemMFID(item: ItemType): String? =
    item.ids.find { it.s == IDKMEHRschemes.LOCAL && it.sl == "MF-ID" }?.value

/**
 * Finds the first MF-ID with local scheme in the TransactionType passed as parameter.
 * @param trn The TransactionType.
 * @return the MF-ID, if any.
 */
fun getTransactionMFID(trn: TransactionType): String? =
    trn.ids.find { it.s == IDKMEHRschemes.LOCAL && it.sl == "MF-ID" }?.value

data class KmehrMessageIndex(
    val transactionIds: PersistentMap<String, Pair<UUID, TransactionType>> = persistentHashMapOf(),
    val transactionChildOf: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val transactionParentOf: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val itemIds: PersistentMap<String, Pair<UUID, ItemType>> = persistentHashMapOf(),
    val serviceFor: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val childOf: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val parentOf: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val approachFor: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val attestationOf: PersistentMap<String, List<String>> = persistentHashMapOf(),
    val formIdMask: UUID = UUID.randomUUID().xor(UUID.randomUUID()), // Ensure that marker bits are set to 0 by xoring two UUIDs
) {
    fun isChildTransaction(trn: TransactionType?) =
        trn?.let { getTransactionMFID(it)?.let { mfid -> childOf.containsKey(mfid) } } ?: false

    fun children(trn: TransactionType?) =
        trn?.let { getTransactionMFID(it)?.let { mfid -> parentOf[mfid] } } ?: listOf()
}

/**
 * Given a Collection of SubContacts, it aggregates all the SubContacts with the same id into a single SubContact.
 * @param scts the Collection of SubContacts
 * @return the updated Collection.
 */
fun simplifySubContacts(scts: Collection<SubContact>): Collection<SubContact> =
    scts.groupBy { it.id }.mapValues { it.value.first().copy(services = it.value.flatMap { svc -> svc.services }) }.values

/**
 * Checks that all the Items and Transactions in the SMF have a unique MF-ID. If two items/transactions have the same
 * ID, we have no other choice but to link to one of them (the last one in this case).
 * @param idGenerator The UUIDGenerator.
 * @return The Updated KmEHR message.
 */
fun Kmehrmessage.performIndexation(idGenerator: UUIDGenerator) = this.folders.fold(KmehrMessageIndex()) { folderKmi, folder ->
    folder.transactions.fold(folderKmi) { transactionKmi, trn ->
        val tmfId = getTransactionMFID(trn)
        val tLinks = trn.headingsAndItemsAndTexts
            .mapNotNull { it as? LnkType }
            .filter { it.type == CDLNKvalues.ISACHILDOF && it.url != null }
            .mapNotNull { lnk ->
                extractMFIDFromUrl(lnk.url)?.let { lnk.type to it }
            }.groupBy { (from, _) -> from }

        val childOfTLinks = tLinks[CDLNKvalues.ISACHILDOF]
        trn.findItems().fold(
            transactionKmi.copy(
                transactionIds = tmfId?.let { transactionKmi.transactionIds + (it to (idGenerator.newGUID() to trn)) } ?: transactionKmi.transactionIds,
                transactionChildOf = if (tmfId != null && !childOfTLinks.isNullOrEmpty()) {
                    transactionKmi.transactionChildOf +
                        (tmfId to childOfTLinks.map { it.second })
                } else {
                    transactionKmi.transactionChildOf
                },
                transactionParentOf = if (tmfId != null && !childOfTLinks.isNullOrEmpty()) {
                    transactionKmi.transactionParentOf +
                        childOfTLinks.map { it.second to (listOf(tmfId) + (transactionKmi.transactionParentOf[it.second] ?: listOf())) }
                } else {
                    transactionKmi.transactionParentOf
                },
            ),
        ) { kmi, item ->
            val mfId = getItemMFID(item)

            val previousVersion = item.lnks
                .find { (it.type == CDLNKvalues.ISANEWVERSIONOF) && it.url != null }
                ?.url
                ?.let { extractMFIDFromUrl(it) }
            val id = previousVersion?.let { kmi.itemIds[it]?.first } ?: idGenerator.newGUID()

            val links = item.lnks
                .filter { (it.type == CDLNKvalues.ISASERVICEFOR || it.type == CDLNKvalues.ISATTESTATIONOF || it.type == CDLNKvalues.ISACHILDOF || it.type == CDLNKvalues.ISAPPROACHFOR) && it.url != null }
                .mapNotNull { lnk ->
                    extractMFIDFromUrl(lnk.url)?.let { lnk.type to it }
                }.groupBy { (from, _) -> from }

            val serviceForLinks = links[CDLNKvalues.ISASERVICEFOR]
            val childOfLinks = links[CDLNKvalues.ISACHILDOF]
            val approachForLinks = links[CDLNKvalues.ISAPPROACHFOR]
            val attestationOfLinks = links[CDLNKvalues.ISATTESTATIONOF]
            kmi.copy(
                itemIds = mfId?.let { kmi.itemIds + (it to (id to item)) } ?: kmi.itemIds,
                serviceFor = if (mfId != null && !serviceForLinks.isNullOrEmpty())
                        kmi.serviceFor + (mfId to serviceForLinks.map { it.second })
                    else kmi.serviceFor,
                childOf = if (mfId != null && !childOfLinks.isNullOrEmpty())
                        kmi.childOf + (mfId to childOfLinks.map { it.second })
                    else kmi.childOf,
                parentOf = if (mfId != null && !childOfLinks.isNullOrEmpty())
                        kmi.parentOf + childOfLinks.map { it.second to (listOf(mfId) + (kmi.parentOf[it.second] ?: listOf())) }
                    else kmi.parentOf,
                approachFor = if (mfId != null && !approachForLinks.isNullOrEmpty())
                        kmi.approachFor + (mfId to approachForLinks.map { it.second })
                    else kmi.approachFor,
                attestationOf = if (mfId != null && !attestationOfLinks.isNullOrEmpty())
                        kmi.attestationOf + (mfId to attestationOfLinks.map { it.second })
                    else kmi.attestationOf
            )
        }
    }
}

private fun selector(
    headingsAndItemsAndTexts: MutableList<Serializable>,
    predicate: ((ItemType) -> Boolean)?,
): List<ItemType> {
    return headingsAndItemsAndTexts.fold(listOf()) { acc, it ->
        when (it) {
            is ItemType -> if (predicate == null || predicate(it)) acc + listOf(it) else acc
            is TextType -> acc
            is HeadingType -> acc + selector(it.headingsAndItemsAndTexts, predicate)
            else -> acc
        }
    }
}

private fun TransactionType.findItem(predicate: ((ItemType) -> Boolean)? = null): ItemType? {
    return selector(this.headingsAndItemsAndTexts, predicate).firstOrNull()
}

private fun TransactionType.findItems(predicate: ((ItemType) -> Boolean)? = null): List<ItemType> {
    return selector(this.headingsAndItemsAndTexts, predicate)
}

private fun AddressTypeBase.getFullAddress(): String {
    val street = "${street ?: ""}${housenumber?.let { " $it" } ?: ""}${postboxnumber?.let { " b $it" } ?: ""}"
    val city = "${zip ?: ""}${city?.let { " $it" } ?: ""}"
    return listOf(street, city, country?.let { it.cd?.value } ?: "").filter { it.isNotBlank() }.joinToString(";")
}
