/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.diarynote

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import java.io.InputStream

/**
 * This interface defines the method to import and export a diary note XML message of the KmEHR standard.
 * This type of transaction corresponds to a very short note contained in the "multidisciplinary diary" of a patient
 * typically stored in a "healthcare safe". It is a free-text field where patients can enter any additional details
 * that they want to communicate to their healthcare provider, such as changes in their symptoms,
 * medication side effects, or other concerns.
 * Further information about this transaction can be found here:
 * https://www.ehealth.fgov.be/standards/kmehr/en/transactions/diary-note
 */
interface DiaryNoteLogic {

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
     * @return a Flow of ByteBuffer containing the XML.
     */
    fun createDiaryNote(pat: Patient, sfks: List<String>, sender: HealthcareParty, recipient: HealthcareParty, note: String?, tags: List<String>, contexts: List<String>, isPsy: Boolean, documentId: String?, attachmentId: String?): Flow<DataBuffer>

    /**
     * Imports a Diary Note XML into iCure.
     * @param inputStream the InputStream containing the XML.
     * @param author the User responsible for the import.
     * @param language the language of the Content.
     * @param mappings
     * @param dest
     * @return a List of ImportResult.
     */
    fun importDiaryNote(inputStream: InputStream, author: User, language: String, dest: Patient? = null, mappings: Map<String, List<ImportMapping>> = HashMap()): List<ImportResult>
}
