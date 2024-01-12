/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.medicationscheme.impl.v20161201

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.medicationscheme.MedicationSchemeLogic
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.services.external.api.AsyncDecrypt
import java.nio.ByteBuffer
import java.time.Instant

@Profile("kmehr")
@Service
class MedicationSchemeLogicImpl(
    private val medicationSchemeExport: MedicationSchemeExport,
    private val medicationSchemeImport: MedicationSchemeImport,
    private val kmehrConfiguration: KmehrConfiguration
) : MedicationSchemeLogic {

    override suspend fun importMedicationSchemeFile(
        inputData: Flow<ByteBuffer>,
        author: User,
        language: String,
        dest: Patient?,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
    ): List<ImportResult> {
        return medicationSchemeImport.importMedicationSchemeFile(inputData, author, language, mappings, saveToDatabase, dest)
    }

    override fun createMedicationSchemeExport(
        patient: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        language: String,
        recipientSafe: String,
        version: Int,
        decryptor: AsyncDecrypt?
    ) =
        medicationSchemeExport.exportMedicationScheme(patient, sfks, sender, language, recipientSafe, version, null, null, decryptor)

    override fun createMedicationSchemeExport(
        patient: Patient,
        sender: HealthcareParty,
        language: String,
        recipientSafe: String,
        version: Int,
        services: List<org.taktik.icure.entities.embed.Service>,
        serviceAuthors: List<HealthcareParty>?,
        timeZone: String?
    ) =
        medicationSchemeExport.exportMedicationScheme(
            patient, listOf(), sender, language, recipientSafe, version, services, null, null,
            Config(
                _kmehrId = System.currentTimeMillis().toString(),
                date = Utils.makeXGC(Instant.now().toEpochMilli(), unsetMillis = false, setTimeZone = false, timeZone = timeZone ?: "Europe/Brussels")!!,
                time = Utils.makeXGC(Instant.now().toEpochMilli(), unsetMillis = true, setTimeZone = false, timeZone = timeZone ?: "Europe/Brussels")!!,
                soft = Config.Software(name = "iCure", version = kmehrConfiguration.kmehrVersion),
                clinicalSummaryType = "",
                defaultLanguage = "en",
            )
        )
}
