/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.be

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.InvoiceLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.be.ehealth.EfactLogic
import org.taktik.icure.services.external.rest.v1.dto.MapOfIdsDto

@RestController
@Profile("kmehr")
@RequestMapping("/rest/v1/be_efact")
@Tag(name = "beefact")
class EfactController(
    private val efactLogic: EfactLogic,
    val sessionLogic: SessionInformationProvider,
    val healthcarePartyLogic: HealthcarePartyLogic,
    private val invoiceLogic: InvoiceLogic,
    private val insuranceLogic: InsuranceLogic,
) {

    @Operation(summary = "create batch and message")
    @PostMapping("/{insuranceId}/{newMessageId}/{numericalRef}")
    fun createBatchAndMessage(
        @PathVariable insuranceId: String,
        @PathVariable newMessageId: String,
        @PathVariable numericalRef: Long,
        @RequestBody ids: MapOfIdsDto,
    ) = mono {
        val hcp = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not a HCP")
        val ins = insuranceLogic.getInsurance(insuranceId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Insurance not found")

        val invoices = ids.mapOfIds.entries.associate {
                    it.key to (invoiceLogic.getInvoices(it.value).toList().takeIf { retrievedInvoices ->
                        retrievedInvoices.size == it.value.size
                    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found") )
                }
        efactLogic.prepareBatch(newMessageId, hcp, ins, invoices)
    }
}
