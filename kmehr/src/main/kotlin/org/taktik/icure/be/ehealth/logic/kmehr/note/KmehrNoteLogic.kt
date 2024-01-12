/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.note

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient

/**
 * This logic defines the method to create a KmEHR note. This is a type of transaction that can be used for multiple
 * purposes, such as free notes or certificates.
 * More information about the XML structure can be found here:
 * https://www.ehealth.fgov.be/standards/kmehr/en/transactions/note
 */
interface KmehrNoteLogic {

    /**
     * Creates a KmEHR note with the information provided and returns a Flow of DataBuffer containing the XML.
     * @param id the ID of the note.
     * @param author the HCP responsible for the creation.
     * @param date a the Fuzzy Date.
     * @param recipientNihii the Nihii of the recipient of the note.
     * @param recipientSsin the Ssin of the recipient of the note.
     * @param recipientFirstName the first name of the recipient of the note.
     * @param recipientLastName the last name of the recipient of the note.
     * @param patient the iCure Patient the note refers to.
     * @param lang the language of the content.
     * @param transactionType the type of the transaction.
     * @param mimeType the MIME type of the document.
     * @param document the document to include in the XML.
     * @return a Flow of Data Buffer.
     */
    suspend fun createNote(
        id: String,
        author: HealthcareParty,
        date: Long,
        recipientNihii: String,
        recipientSsin: String,
        recipientFirstName: String,
        recipientLastName: String,
        patient: Patient,
        lang: String,
        transactionType: String,
        mimeType: String,
        document: ByteArray,
    ): Flow<DataBuffer>
}
