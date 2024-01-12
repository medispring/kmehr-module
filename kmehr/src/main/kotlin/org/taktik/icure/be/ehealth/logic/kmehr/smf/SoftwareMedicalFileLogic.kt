/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.smf

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.CheckSMFPatientResult
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.services.external.api.AsyncDecrypt
import org.taktik.icure.services.external.http.websocket.AsyncProgress
import java.nio.ByteBuffer

/**
 * This interface defines the methods to Import, Export and validate SMF messages.
 */
interface SoftwareMedicalFileLogic {

    /**
     * Exports the medical data of the Patient in the SMF format, that returns as a Flow of DataBuffer.
     * @param patient the iCure Patient.
     * @param sfks the Patient Secret Foreign Keys.
     * @param sender the HCP responsible for the export.
     * @param language the language of the Content.
     * @param decryptor the Decryptor for the Documents and other entities.
     * @param progressor Used to show the progress of the operation.
     * @param config the Config.
     * @return a Flow of DataBuffer containing the XML message.
     */
    fun createSmfExport(patient: Patient, sfks: List<String>, sender: HealthcareParty, language: String, decryptor: AsyncDecrypt?, progressor: AsyncProgress?, config: Config): Flow<DataBuffer>

    /**
     * Converts an SMF (Software Medical File) XML message to a set of Contacts, SubContacts, Services, HealthElements,
     * Documents, and Forms of the iCure Data Model.
     * @param inputData the ByteArray containing the XML message.
     * @param author the User responsible for the import.
     * @param language the language of the Content inside the iCure entities.
     * @param dryRun if true, does not save to the db the newly created entities.
     * @param mappings
     * @param dest the Patient object of the SMF.
     * @return a List of ImportResult.
     */
    suspend fun importSmfFile(
        inputData: ByteArray,
        author: User,
        language: String,
        dryRun: Boolean,
        dest: Patient? = null,
        mappings: Map<String, List<ImportMapping>> = emptyMap(),
    ): List<ImportResult>

    /**
     * Checks if all the patients in an SMF message exist in the db.
     * @param inputData the SMF message as a Flow of Byte Buffer.
     * @param author the User responsible for the check.
     * @param dest if not null, no Patient is retrieved from the db and this is used.
     * @return a List of CheckSMFPatientResult
     */
    suspend fun checkIfSMFPatientsExists(
        inputData: Flow<ByteBuffer>,
        author: User,
        dest: Patient?,
    ): List<CheckSMFPatientResult>
}
