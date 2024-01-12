/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.smf.impl.v23g

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.smf.SoftwareMedicalFileLogic
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.CheckSMFPatientResult
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.services.external.api.AsyncDecrypt
import org.taktik.icure.services.external.http.websocket.AsyncProgress
import java.nio.ByteBuffer

@Profile("kmehr")
@Service
class SoftwareMedicalFileLogicImpl(
    private val softwareMedicalFileExport: SoftwareMedicalFileExport,
    private val softwareMedicalFileImport: SoftwareMedicalFileImport,
) : SoftwareMedicalFileLogic {

    override suspend fun importSmfFile(
        inputData: ByteArray,
        author: User,
        language: String,
        dryRun: Boolean,
        dest: Patient?,
        mappings: Map<String, List<ImportMapping>>,
    ): List<ImportResult> =
        softwareMedicalFileImport.importSMF(inputData, author, language, !dryRun, mappings, dest)

    override suspend fun checkIfSMFPatientsExists(
        inputData: Flow<ByteBuffer>,
        author: User,
        dest: Patient?,
    ): List<CheckSMFPatientResult> =
        softwareMedicalFileImport.checkIfSMFPatientsExists(inputData, author, dest)

    override fun createSmfExport(patient: Patient, sfks: List<String>, sender: HealthcareParty, language: String, decryptor: AsyncDecrypt?, progressor: AsyncProgress?, config: Config): Flow<DataBuffer> =
        softwareMedicalFileExport.exportSMF(patient, sfks, sender, language, decryptor, progressor, config)
}
