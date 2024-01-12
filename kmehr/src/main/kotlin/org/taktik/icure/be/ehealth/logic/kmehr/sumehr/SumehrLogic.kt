/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.sumehr

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.be.ehealth.dto.SumehrStatus
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.entities.embed.Partnership
import org.taktik.icure.entities.embed.PatientHealthCareParty
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.services.external.api.AsyncDecrypt
import java.nio.ByteBuffer

/**
 * All the classes implementing this interface will handle the interoperability with SumHER.
 *
 * The Summarised Electronic Health Record (or SumEHR) is the minimal set of data that a physician needs to understand
 * the medical status of the patient in a few minutes and to ensure the continuity of care.
 * (https://www.ehealth.fgov.be/standards/kmehr/en/transactions/summarised-electronic-healthcare-record-v11)
 */
interface SumehrLogic {

    /**
     * This method checks if the latest active SumEHR related to a Patient and a Doctor is valid. To do so, it compares
     * the MD5 signature of the latest SumEHR to the one obtained by retrieving all the Services and HealthElements that
     * are part of the SumEHR and calculating their MD5 signature.
     * @param hcPartyId the HCP involved in the SumEHR.
     * @param patient the Patient involved in the SumEHR.
     * @param patientSecretForeignKeys the Secret Foreign Keys of the Patient.
     * @param excludedIds the ID of the Services and HealthElements to exclude
     * @param includeIrrelevantInformation whether to include entities that are irrelevant.
     * @return a SumEHR status: [SumehrStatus.uptodate] if the signatures match, [SumehrStatus.outdated] otherwise
     */
    suspend fun isSumehrValid(hcPartyId: String, patient: Patient, patientSecretForeignKeys: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean): SumehrStatus

    /**
     * Creates a SumEHR XML message populated with all the medical data of a Patient and emits it as a flow
     * of DataBuffer.
     * @param pat the Patient.
     * @param sfks the Secret Foreign Keys of the Patient.
     * @param sender the HCP responsible for the SumEHR.
     * @param recipient the HCP to send the SumEHR to.
     * @param language the content language of the HealthElement.
     * @param comment
     * @param excludedIds the ids of the Services and HealthElements to exclude.
     * @param includeIrrelevantInformation whether to include irrelevant or inactive Services and HealthElements.
     * @param decryptor the decryptor for Services and HealthElements.
     * @param services if not null, uses these Services instead of retrieving them from the db.
     * @param healthElements if not null, uses these HealthElements instead of retrieving them from the db.
     * @param config the Config used to create PatientTypes.
     * @return a flow of DataBuffer containing the SumEHR XML.
     */
    fun createSumehr(pat: Patient, sfks: List<String>, sender: HealthcareParty, recipient: HealthcareParty, language: String, comment: String, excludedIds: List<String>, includeIrrelevantInformation: Boolean, decryptor: AsyncDecrypt?, services: List<Service>?, healthElements: List<HealthElement>?, config: Config): Flow<DataBuffer>

    /**
     * Creates a SumEHR XML message populated with all the medical data of a Patient, then validates it using the
     * EhValidator library and emits it as a flow of [DataBuffer].
     * @param pat the Patient.
     * @param sfks the Secret Foreign Keys of the Patient.
     * @param sender the HCP responsible for the SumEHR.
     * @param recipient the HCP to send the SumEHR to.
     * @param language the content language of the HealthElement.
     * @param comment
     * @param excludedIds the ids of the Services and HealthElements to exclude.
     * @param includeIrrelevantInformation whether to include irrelevant or inactive Services and HealthElements.
     * @param decryptor the decryptor for Services and HealthElements.
     * @param services if not null, uses these Services instead of retrieving them from the db.
     * @param healthElements if not null, uses these HealthElements instead of retrieving them from the db.
     * @param config the Config used to create PatientTypes.
     * @return a flow of DataBuffer containing the SumEHR XML.
     */
    fun validateSumehr(pat: Patient, sfks: List<String>, sender: HealthcareParty, recipient: HealthcareParty, language: String, comment: String, excludedIds: List<String>, includeIrrelevantInformation: Boolean, decryptor: AsyncDecrypt?, services: List<Service>?, healthElements: List<HealthElement>?, config: Config): Flow<DataBuffer>

    /**
     * Gets all the active Services, the one related to Medications and Vaccines for a HCP hierarchy and Patients.
     * @param hcPartyId the HCP at the base of the hierarchy.
     * @param sfks the list of Patient Secret Foreign Keys.
     * @param excludedIds the list of ids of Services to exclude.
     * @param includeIrrelevantInformation whether to include Services that are inactive or irrelevant.
     * @param decryptor the decryptor for the Services.
     * @return a list of Services.
     */
    suspend fun getAllServices(hcPartyId: String, sfks: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean, decryptor: AsyncDecrypt?): List<Service>

    /**
     * Returns the list of HealthElements provided by the user, if not null, otherwise retrieves them based on a HCP
     * hierarchy and Patient SFKs passed as parameters. It filters out the irrelevant and the excluded HealthElements.
     * @param hcPartyId the id of the HCP at the base of the hierarchy.
     * @param sfks the Secret Foreign Keys of the Patients related to the HealthElements to retrieve.
     * @param excludedIds the id of the HealthElements to exclude.
     * @param includeIrrelevantInformation whether to include HealthElements with irrelevant information.
     * @return a list of HealthElements.
     */
    suspend fun getHealthElements(hcPartyId: String, sfks: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean): List<HealthElement>

    /**
     * Gets all the partnerships from a Patient but the ones to exclude.
     * @param excludedIds the ids of the partnerships to exclude.
     * @param patientId the id of the Patient
     * @return the list of Partnership related to the Patient
     */
    suspend fun getContactPeople(excludedIds: List<String>, patientId: String): List<Partnership>

    /**
     * Gets all the healthcare parties from a Patient but the ones to exclude.
     * @param excludedIds the ids of the HCPs to exclude.
     * @param patientId the id of the Patient
     * @return the list of HCPs related to the Patient
     */
    suspend fun getPatientHealthcareParties(excludedIds: List<String>, patientId: String): List<PatientHealthCareParty>

    /**
     * This method calculates the MD5 signature for a set of Services and HealthElements. All the Services and
     * HealthElements retrieved are the ones related to the HCP passed as parameter and to the Patients whose Secret
     * Foreign Keys are passed as parameter.
     * The MD5 signature is calculated by sorting and joining in a string the IDs of all the retrieved entities.
     * @param hcPartyId the HCP related to the entities.
     * @param patient the Patient related to the entities.
     * @param patientSecretForeignKeys the Secret Foreign keys used to filter the entities.
     * @param excludedIds a list of IDs of entities to exclude.
     * @param includeIrrelevantInformation whether to include entities that are irrelevant.
     * @return the MD5 signature
     */
    suspend fun getSumehrMd5(hcPartyId: String, patient: Patient, patientSecretForeignKeys: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean): String

    /**
     * Converts a SumEHR XML entities into a set of iCure entities. For each folder in the SumEHR, it creates a new
     * Contact with HealthElements or Services. If the HCP and Patients in the SumEHR do not exist, it creates them and
     * may save them to the database.
     * @param inputData the SumEHR XML as a flow of bytes
     * @param author the HCP User responsible for the import
     * @param mappings
     * @param saveToDatabase whether to save the new HCP and Patients to the database
     * @param dest a Patient to use instead of the ones contained in the SumEHR
     * @return a list of ImportResult, one for each folder in the SumEHR
     */
    suspend fun importSumehr(inputData: Flow<ByteBuffer>, author: User, language: String, dest: Patient? = null, mappings: Map<String, List<ImportMapping>> = HashMap(), saveToDatabase: Boolean): List<ImportResult>

    /**
     * Converts a SumEHR XML entities into a set of iCure entities. For each folder in the SumEHR, it creates a new
     * Contact with HealthElements or Services. If the HCP and Patients in the SumEHR do not exist, it creates them and
     * may save them to the database.
     * @param inputData the SumEHR XML as a flow of bytes
     * @param itemId
     * @param author the HCP User responsible for the import
     * @param mappings
     * @param saveToDatabase whether to save the new HCP and Patients to the database
     * @param dest a Patient to use instead of the ones contained in the SumEHR
     * @return a list of ImportResult, one for each folder in the SumEHR
     */
    suspend fun importSumehrByItemId(inputData: Flow<ByteBuffer>, itemId: String, author: User, language: String, dest: Patient? = null, mappings: Map<String, List<ImportMapping>> = HashMap(), saveToDatabase: Boolean): List<ImportResult>
}
