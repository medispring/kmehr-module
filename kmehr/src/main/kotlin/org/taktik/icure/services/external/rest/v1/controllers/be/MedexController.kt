/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.be

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.taktik.icure.be.ehealth.logic.kmehr.medex.MedexLogic
import org.taktik.icure.services.external.rest.v1.dto.be.MedexInfoDto
import org.taktik.icure.services.external.rest.v1.mapper.HealthcarePartyMapper
import org.taktik.icure.services.external.rest.v1.mapper.PatientMapper

@RestController
@Profile("kmehr")
@RequestMapping("/rest/v1/medex")
@Tag(name = "medex")
class MedexController(
    private val medexLogic: MedexLogic,
    private val healthcarePartyMapper: HealthcarePartyMapper,
    private val patientMapper: PatientMapper,
) {

    @Operation(summary = "Generate a Medex XML String")
    @PostMapping("/generate", produces = [MediaType.APPLICATION_XML_VALUE])
    fun generateMedex(@RequestBody infos: MedexInfoDto) = mono {
        medexLogic.createMedex(
            healthcarePartyMapper.map(infos.author!!),
            patientMapper.map(infos.patient!!),
            infos.patientLanguage,
            infos.incapacityType,
            infos.incapacityReason,
            infos.outOfHomeAllowed,
            infos.certificateDate,
            infos.contentDate,
            infos.beginDate,
            infos.endDate,
            infos.diagnosisICD,
            infos.diagnosisICPC,
            infos.diagnosisDescr,
        )
    }
}
