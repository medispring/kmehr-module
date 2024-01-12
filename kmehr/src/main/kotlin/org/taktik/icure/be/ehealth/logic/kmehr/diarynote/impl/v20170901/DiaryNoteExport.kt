/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.diarynote.impl.v20170901

import kotlinx.coroutines.flow.flow
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.CodeLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHR
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.*
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.emitMessage
import org.taktik.icure.be.ehealth.logic.kmehr.getSignature
import org.taktik.icure.be.ehealth.logic.kmehr.v20170901.KmehrExport
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import java.time.Instant

@Profile("kmehr")
@Service("dairyNoteExport")
class DiaryNoteExport(
    codeLogic: CodeLogic,
    documentLogic: DocumentLogic,
    kmehrConfiguration: KmehrConfiguration
) : KmehrExport(codeLogic, documentLogic, kmehrConfiguration) {
    override val log = LogFactory.getLog(DiaryNoteExport::class.java)

    fun getMd5(patient: Patient): String {
        val signatures = ArrayList(listOf(patient.getSignature()))
        val sorted = signatures.sorted()
        return DigestUtils.md5Hex(sorted.joinToString(","))
    }

    /**
     * Creates a DiaryNote XML message for the patient, including the information passed as parameter.
     * @param pat the iCure Patient.
     * @param sfks the Patient Secret Foreign Keys. Used to decrypt the document.
     * @param sender the HCP responsible for the export.
     * @param recipient the HCP recipient of the message
     * @param note the note to add in the Folder.
     * @param tags the tags to add in the Folder.
     * @param contexts
     * @param isPsy true if the note is related to the psychiatry department.
     * @param documentId the iCure Document to include in the Folder.
     * @param attachmentId the id of the Document Attachment to include in the Folder.
     * @param config the Config.
     * @return a Flow of ByteBuffer containing the XML.
     */
    suspend fun createDiaryNote(
        pat: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        recipient: HealthcareParty?,
        note: String?, // should be in format like: <?xml version=\"1.0\" encoding=\"UTF-16\"?>\n<p xmlns=\"http://www.ehealth.fgov.be/standards/kmehr/schema/v1\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">dit een note met alle type en context selected</p>
        tags: List<String>,
        contexts: List<String>,
        isPsy: Boolean,
        documentId: String?,
        attachmentId: String?,
        config: Config = Config(
            _kmehrId = System.currentTimeMillis().toString(),
            date = makeXGC(Instant.now().toEpochMilli())!!,
            time = Utils.makeXGC(Instant.now().toEpochMilli(), true)!!,
            soft = Config.Software(name = "iCure", version = kmehrConfig.kmehrVersion),
            clinicalSummaryType = "",
            defaultLanguage = "en",
            ),
    ) = flow {
        val message = initializeMessage(sender, config)
        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(
                    recipient?.let { createParty(it, emptyList()) } ?:
                    createParty(emptyList(), listOf(CDHCPARTY().apply { s = CDHCPARTYschemes.CD_APPLICATION; sv = "1.0" }), "gp-software-migration")
                )
            }
        )

        val folder = FolderType()
        folder.ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = 1.toString() })
        folder.patient = makePerson(pat, config)
        fillPatientFolder(folder, pat, sfks, sender, config, note, tags, contexts, isPsy, documentId, attachmentId)
        message.folders.add(folder)

        fillPatientFolder(folder, pat, sfks, sender, config, note, tags, contexts, isPsy, documentId, attachmentId)
        emitMessage(message.apply { folders.add(folder) }).collect { emit(it) }
    }

    /**
     * Returns a CD-TRANSACTION dn from a String.
     * @param context the String.
     * @return the CD-TRANSACTION dn.
     */
    private fun dnFromContext(context: String): String =
        when (context) {
            "psichronilux" -> "CHRONILUX"
            "psipact" -> "PACT"
            "psiresinam" -> "RéSiNam"
            "psi3c4h" -> "3C4H"
            "psirelian" -> "RéLIAN"
            else -> ""
        }

    /**
     * Fills the Patient folder of the note with the data from a TransactionType and an iCure Document.
     * @param folder the FolderType.
     * @param p the iCure Patient.
     * @param sfks the Patient Secret Foreign Keys. Used to decrypt the document.
     * @param sender the HCP responsible for the export.
     * @param config the Config.
     * @param note the note to add in the Folder.
     * @param tags the tags to add in the Folder.
     * @param contexts
     * @param isPsy true if the note is related to the psychiatry department.
     * @param documentId the iCure Document to include in the Folder.
     * @param attachmentId the id of the Document Attachment to include in the Folder.
     * @return a FolderType.
     */
    private suspend fun fillPatientFolder(
        folder: FolderType,
        p: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        config: Config,
        note: String?,
        tags: List<String>,
        contexts: List<String>,
        isPsy: Boolean,
        documentId: String?,
        attachmentId: String?
    ) : FolderType {
        val trn = TransactionType().apply {
            cds.add(CDTRANSACTION().apply { s(CDTRANSACTIONschemes.CD_TRANSACTION); value = "diarynote" })
            author = AuthorType().apply {
                hcparties.add(createParty(sender, emptyList()))
                if (isPsy) {
                    hcparties.add(
                        HcpartyType().apply {
                            cds.add(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = "deptpsychiatry" })
                        }
                    )
                }
            }
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = "1" })
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "iCure-Item"; sv = config.soft?.version ?: "1.0"; value = p.id.replace("-".toRegex(), "").substring(0, 8) + "." + System.currentTimeMillis() })
            tags.forEach { tag -> cds.add(CDTRANSACTION().apply { s(CDTRANSACTIONschemes.CD_DIARY); value = tag }) }
            contexts.forEach { context -> cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.LOCAL; sv = "1.0"; sl = "CD-RSW-CONTEXT"; value = context; dn = dnFromContext(context) }) }
            makeXGC(System.currentTimeMillis()).let { date = it; time = it }
            isIscomplete = true
            isIsvalidated = true
        }
        folder.transactions.add(trn)
        if (documentId?.isNotEmpty() == true && attachmentId?.isNotEmpty() == true) {
            val document = documentLogic.getDocument(documentId)
            val attachment = document?.let { documentLogic.getAndDecryptMainAttachment(it.id, sfks) }
            if (attachment != null) {
                trn.headingsAndItemsAndTexts.add(LnkType().apply { type = CDLNKvalues.MULTIMEDIA; mediatype = documentMediaType(document); value = attachment })
            }
        }
        if ((note?.length ?: 0) > 0) {
            val t = TextWithLayoutType().apply { l = sender.languages.firstOrNull() ?: "fr" }
            t.content.add(note)
            trn.headingsAndItemsAndTexts.add(t)
        }
        // Remove empty headings
        val iterator = folder.transactions[0].headingsAndItemsAndTexts.iterator()
        while (iterator.hasNext()) {
            val h = iterator.next()
            if (h is HeadingType) {
                if (h.headingsAndItemsAndTexts.size == 0) {
                    iterator.remove()
                }
            }
        }
        return folder
    }
}
