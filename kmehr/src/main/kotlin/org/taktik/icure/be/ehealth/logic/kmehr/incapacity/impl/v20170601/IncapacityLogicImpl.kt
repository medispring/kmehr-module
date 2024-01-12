package org.taktik.icure.be.ehealth.logic.kmehr.incapacity.impl.v20170601

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.incapacity.IncapacityLogic
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.domain.be.kmehr.IncapacityExportInfo
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import java.time.Instant

@Profile("kmehr")
@Service
class IncapacityLogicImpl(
    private val incapacityExport: IncapacityExport,
    private val kmehrConfiguration: KmehrConfiguration
) : IncapacityLogic {

    override fun createIncapacityExport(
        patient: Patient,
        sender: HealthcareParty,
        language: String,
        exportInfo: IncapacityExportInfo,
        timeZone: String?,
    ) =
        incapacityExport.exportIncapacity(
            patient,
            sender,
            language,
            exportInfo,
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
