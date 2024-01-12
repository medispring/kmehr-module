/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.medicationscheme

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.services.external.api.AsyncDecrypt
import java.nio.ByteBuffer

/**
 * This interface defines the method used to Import and Export the Medication Scheme XML message from and to iCure.
 * The Medication Scheme is a structured representation of the medication that a patient is taking,
 * or has taken in the past. The Medication Scheme includes information such as the name of the medication,
 * dosage, frequency of use, and the start and end dates of the medication.
 */
interface MedicationSchemeLogic {

    /**
     * Converts a Medication Scheme XML message to a set of Contacts, SubContacts, Services, HealthElements,
     * Documents, and Forms of the iCure Data Model.
     * @param inputData the ByteArray containing the XML message.
     * @param author the User responsible for the import.
     * @param language the language of the Content inside the iCure entities.
     * @param saveToDatabase whether to save in the database the new entities created.
     * @param mappings
     * @param dest the Patient object of the SMF.
     * @return a List of ImportResult.
     */
    suspend fun importMedicationSchemeFile(inputData: Flow<ByteBuffer>, author: User, language: String, dest: Patient?, mappings: Map<String, List<ImportMapping>>, saveToDatabase: Boolean): List<ImportResult>

    /**
     * Exports a Medication Scheme XML Message that retrieves the Services from the db using the Patient, Patient
     * Secret Foreign Keys, and HCP provided. If a Decryptor is passed, it uses only the Services that can decrypt.
     * @param patient the iCure Patient.
     * @param sfks the Patient Secret Foreign Keys.
     * @param sender the HCP responsible for the export.
     * @param language the language of the Content.
     * @param recipientSafe
     * @param version the message version.
     * @param decryptor the Decryptor for the Documents and other entities.
     * @return a Flow of DataBuffer containing the XML message.
     */
    fun createMedicationSchemeExport(
        patient: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        language: String,
        recipientSafe: String,
        version: Int,
        decryptor: AsyncDecrypt?
    ): Flow<DataBuffer>

    /**
     * Exports a Medication Scheme XML Message. It uses the Services passed as parameter.
     * @param patient the iCure Patient.
     * @param sender the HCP responsible for the export.
     * @param language the language of the Content.
     * @param recipientSafe
     * @param version the message version.
     * @param services the Services to include in the message.
     * @param serviceAuthors the authors of the Services.
     * @param timeZone
     * @return a Flow of DataBuffer containing the XML message.
     */
    fun createMedicationSchemeExport(
        patient: Patient,
        sender: HealthcareParty,
        language: String,
        recipientSafe: String,
        version: Int,
        services: List<Service>,
        serviceAuthors: List<HealthcareParty>?,
        timeZone: String?
    ): Flow<DataBuffer>
}
