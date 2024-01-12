/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.format.logic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.taktik.commons.uti.UTI
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTYschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDMESSAGEvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.LnkType
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENTschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType
import org.taktik.icure.be.format.logic.KmehrReportLogic
import org.taktik.icure.domain.result.ResultInfo
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.ServiceLink
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.utils.FuzzyValues
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.xml.bind.JAXBContext

@Profile("kmehr")
@org.springframework.stereotype.Service
class KmehrReportLogicImpl(
    healthcarePartyLogic: HealthcarePartyLogic,
    documentLogic: DocumentLogic,
    val contactLogic: ContactLogic
) : GenericResultFormatLogicImpl(healthcarePartyLogic, documentLogic), KmehrReportLogic {
    internal var log = LogFactory.getLog(this.javaClass)

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, text: String?): Flow<DataBuffer> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, mimeType: String?, content: ByteArray?): Flow<DataBuffer> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    @Throws(IOException::class)
    override suspend fun canHandle(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): Boolean {
        val msg = extractMessage(doc, encKeys, cachedAttachment)

        val isSmfOrPmf = msg?.header?.standard?.specialisation?.cd?.value?.let {
            it == CDMESSAGEvalues.GPPATIENTMIGRATION || it == CDMESSAGEvalues.GPSOFTWAREMIGRATION
        } ?: false

        return !isSmfOrPmf && msg?.folders?.any { folderType ->
            folderType.transactions.any { transactionType ->
                transactionType.cds.any {
                    it.s == CDTRANSACTIONschemes.CD_TRANSACTION && (it.value == "contactreport" || it.value == "note"
                            || it.value == "report" || it.value == "prescription" || it.value == "request")
                }
            }
        } ?: false
    }

    @Throws(IOException::class)
    override suspend fun getInfos(doc: Document, full: Boolean, language: String, encKeys: List<String>, cachedAttachment: ByteArray?): List<ResultInfo> {
        val msg = extractMessage(doc, encKeys, cachedAttachment)

        return msg?.folders?.flatMap { f ->
            f.transactions.filter { t -> t.cds.any { it.s == CDTRANSACTIONschemes.CD_TRANSACTION &&
                    (it.value == "contactreport" || it.value == "note" || it.value == "report"
                            || it.value == "prescription" || it.value == "request") }
            }.map { t ->
                    ResultInfo().apply {
                        ssin = f.patient.ids.find { it.s == IDPATIENTschemes.INSS }?.value
                        lastName = f.patient.familyname
                        firstName = f.patient.firstnames.firstOrNull()
                        dateOfBirth = f.patient.birthdate.date?.let { FuzzyValues.getFuzzyDate(LocalDateTime.of(it.year, it.month, it.day, 0, 0), ChronoUnit.DAYS) }
                        sex = f.patient.sex?.cd?.value?.value() ?: "unknown"
                        documentId = doc.id
                        protocol = t.ids.find { it.s == IDKMEHRschemes.LOCAL }?.value
                        complete = t.isIscomplete
                        labo = getAuthorDescription(t)
                        demandDate = demandEpochMillis(t)
                        codes = mutableListOf(CodeStub.from("CD-TRANSACTION", "report", "1"))
                    }
                }
        } ?: listOf()
    }

    override suspend fun doImport(language: String, doc: Document, hcpId: String?, protocolIds: List<String>, formIds: List<String>, planOfActionId: String?, ctc: Contact, encKeys: List<String>, cachedAttachment: ByteArray?): Contact? {
        val msg = extractMessage(doc, encKeys, cachedAttachment)
        val subContactsAndServices = msg?.folders?.flatMap { f ->
            f.transactions.filter { t ->
                t.ids.any { it.s == IDKMEHRschemes.LOCAL && protocolIds.contains(it.value) }
            }.map { t ->
                val protocolId = t.ids.find { it.s == IDKMEHRschemes.LOCAL }?.value
                val demandTimestamp = demandEpochMillis(t)


                val textItems = t.headingsAndItemsAndTexts.filterIsInstance(TextType::class.java)
                val s = Service(
                        id = uuidGen.newGUID().toString(),
                        content = mapOf(language to Content(stringValue = t.headingsAndItemsAndTexts.filterIsInstance(TextType::class.java).joinToString(separator = "\n") { it.value })),
                        label = "Protocol",
                        valueDate = demandTimestamp?.let { FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()), ChronoUnit.SECONDS) },
                    ).takeIf { textItems.isNotEmpty() }

                val docServices = t?.headingsAndItemsAndTexts?.filterIsInstance(LnkType::class.java)?.map { lnk ->
                    Service(
                        id = uuidGen.newGUID().toString(),
                        content = mapOf(
                            language to Content(
                                documentId = ctc.responsible?.let {
                                    val utis = UTI.utisForMimeType(lnk.mediatype.value()).toList()
                                    documentLogic.createDocument(
                                        Document(
                                            id = uuidGen.newGUID().toString(),
                                            author = ctc.author,
                                            responsible = ctc.responsible,
                                            created = demandTimestamp ?: ctc.created,
                                            modified = demandTimestamp ?: ctc.created,
                                            name = "Protocol Document",
                                        ),
                                        true,
                                    )?.let { createdDocument ->
                                        documentLogic.updateAttachments(
                                            createdDocument,
                                            mainAttachmentChange = DataAttachmentChange.CreateOrUpdate(
                                                flowOf(DefaultDataBufferFactory.sharedInstance.wrap(lnk.value)),
                                                lnk.value.size.toLong(),
                                                utis.takeIf { x -> x.isNotEmpty() }?.map { x -> x.identifier } ?: listOf("com.adobe.pdf"),
                                            )
                                        )
                                    }?.id
                                }
                            )
                        ),
                        label = "Protocol Document",
                        valueDate = demandTimestamp?.let { FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()), ChronoUnit.SECONDS) },
                    )
                } ?: listOf()

                val services = (s?.let { listOf(it) } ?: listOf()) + docServices
                SubContact(
                    id = uuidGen.newGUID().toString(),
                    responsible = hcpId,
                    descr = getAuthorDescription(t),
                    protocol = protocolId,
                    planOfActionId = planOfActionId,

                    status = SubContact.STATUS_PROTOCOL_RESULT or SubContact.STATUS_UNREAD or (if (t.isIscomplete) SubContact.STATUS_COMPLETE else 0),
                    formId = protocolIds.indexOf(protocolId).let { formIds.get(it) },
                    services = services.map { ServiceLink(it.id) },
                ) to services
            }
        } ?: listOf()
        return contactLogic.modifyEntities(listOf(
            ctc.copy(
                subContacts = ctc.subContacts + subContactsAndServices.map { it.first },
                services = ctc.services + subContactsAndServices.flatMap { it.second }
            )
        )).firstOrNull()
    }

    /**
     * Extracts the main attachment from the iCure Document passed as parameter, tries to decrypt it and returns it as
     * a KmEHR message.
     * @param doc the iCure Document.
     * @param encKeys the encryption keys to decrypt the attachment.
     * @param cachedAttachment if not null, this is used instead of retrieving the main attachment
     * @return a KmEHR message.
     */
    private suspend fun extractMessage(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?) =
        try {
            JAXBContext.newInstance(Kmehrmessage::class.java).createUnmarshaller().unmarshal(getBufferedReader(doc, encKeys, cachedAttachment)) as Kmehrmessage
        } catch (e: Exception) {
            null
        }

    /**
     * Extracts the demand date from a TransactionType.
     * @param t the TransactionType.
     * @return a timestamp.
     */
    private fun demandEpochMillis(t: TransactionType) =
        t.date?.let {
            LocalDateTime.of(
                t.date.year,
                t.date.month,
                t.date.day,
                t.time?.hour
                    ?: 0,
                t.time?.minute ?: 0,
            ).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        }

    /**
     * Extracts the author description from a TransactionType.
     * @param t the TransactionType.
     * @return the author description.
     */
    private fun getAuthorDescription(t: TransactionType) =
        t.author.hcparties
            .associateBy { hcp -> hcp.cds.find { it.s == CDHCPARTYschemes.CD_HCPARTY }?.value ?: "unknown" }
            .let { pts ->
                pts.keys.filter { it != "orghospital" && it != "persphysician" }
                    .plus("orghospital")
                    .joinToString(" - ") { pts[it]?.name ?: it }
            }
}
