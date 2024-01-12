/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.controllers.be

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.be.format.logic.HealthOneLogic
import org.taktik.icure.be.format.logic.KmehrReportLogic
import org.taktik.icure.be.format.logic.MedidocLogic
import org.taktik.icure.utils.FuzzyValues

@Profile("kmehr")
@RestController("resultExportControllerV2")
@RequestMapping("/rest/v2/be_result_export")
@Tag(name = "beresultexport")
class ResultExportController(
    private val healthOneLogic: HealthOneLogic,
    private val medidocLogic: MedidocLogic,
    private val kmehrReportLogic: KmehrReportLogic,
    private val patientLogic: PatientLogic,
    private val healthcarePartyLogic: HealthcarePartyLogic,
) {

    @Operation(summary = "Export data", responses = [ApiResponse(responseCode = "200", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])])
    @PostMapping("/medidoc/{fromHcpId}/{toHcpId}/{patId}/{date}/{ref}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun exportMedidoc(
        @PathVariable fromHcpId: String,
        @PathVariable toHcpId: String,
        @PathVariable patId: String,
        @PathVariable date: Long,
        @PathVariable ref: String,
        @RequestBody bodyText: ByteArray,
    ) = mono {
        DefaultDataBufferFactory().join(
            medidocLogic.doExport(
                healthcarePartyLogic.getHealthcareParty(fromHcpId),
                healthcarePartyLogic.getHealthcareParty(toHcpId),
                patientLogic.getPatient(patId),
                FuzzyValues.getDateTime(date),
                ref,
                String(bodyText, Charsets.UTF_8)).toList()
        ).asByteBuffer()
    }

    @Operation(summary = "Export data", responses = [ApiResponse(responseCode = "200", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])])
    @PostMapping("/hl1/{fromHcpId}/{toHcpId}/{patId}/{date}/{ref}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun exportHealthOne(
        @PathVariable fromHcpId: String,
        @PathVariable toHcpId: String,
        @PathVariable patId: String,
        @PathVariable date: Long,
        @PathVariable ref: String,
        @RequestBody bodyText: ByteArray,
    ) = mono {
        DefaultDataBufferFactory().join(
            healthOneLogic.doExport(
                healthcarePartyLogic.getHealthcareParty(fromHcpId),
                healthcarePartyLogic.getHealthcareParty(toHcpId),
                patientLogic.getPatient(patId),
                FuzzyValues.getDateTime(date),
                ref,
                String(bodyText, Charsets.UTF_8)
            ).toList()).asByteBuffer()
    }

    @Operation(summary = "Export data", responses = [ApiResponse(responseCode = "200", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])])
    @PostMapping("/kmehrreport/{fromHcpId}/{toHcpId}/{patId}/{date}/{ref}", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun exportKmehrReport(
        @PathVariable fromHcpId: String,
        @PathVariable toHcpId: String,
        @PathVariable patId: String,
        @PathVariable date: Long,
        @PathVariable ref: String,
        @RequestParam(required = false) mimeType: Boolean?,
        @RequestBody bodyText: ByteArray,
    ) = mono {
        DefaultDataBufferFactory().join(
            kmehrReportLogic.doExport(
                healthcarePartyLogic.getHealthcareParty(fromHcpId),
                healthcarePartyLogic.getHealthcareParty(toHcpId),
                patientLogic.getPatient(patId),
                FuzzyValues.getDateTime(date),
                ref,
                String(bodyText, Charsets.UTF_8)).toList()
        ).asByteBuffer()
    }
}
