/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.controllers.be

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.StringUtils.isBlank
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.be.format.logic.MultiFormatLogic
import org.taktik.icure.services.external.rest.v1.dto.ContactDto
import org.taktik.icure.services.external.rest.v1.mapper.ContactMapper
import org.taktik.icure.services.external.rest.v1.mapper.ResultInfoMapper

@RestController
@Profile("kmehr")
@RequestMapping("/rest/v1/be_result_import")
@Tag(name = "beresultimport")
class ResultImportController(
    private val multiFormatLogic: MultiFormatLogic,
    private val documentLogic: DocumentLogic,
    private val resultInfoMapper: ResultInfoMapper,
    private val contactMapper: ContactMapper,
) {

    @Operation(summary = "Can we handle this document")
    @GetMapping("/canhandle/{id}")
    fun canHandle(
        @PathVariable id: String,
        @RequestParam enckeys: String,
    ) = mono {
        documentLogic.getDocument(id)?.let { multiFormatLogic.canHandle(it, if (isBlank(enckeys)) listOf() else enckeys.split(',')) }
    }

    @Operation(summary = "Extract general infos from document")
    @GetMapping("/infos/{id}")
    fun getInfos(
        @PathVariable id: String,
        @RequestParam language: String,
        @RequestParam enckeys: String,
        @RequestParam(required = false) full: Boolean?,
    ) = mono {
        val doc = documentLogic.getDocument(id)
        doc?.let {
            multiFormatLogic.getInfos(
                it,
                full ?: false,
                language,
                if (isBlank(enckeys)) listOf() else enckeys.split(',')
            ).map(resultInfoMapper::map)
        }
    }

    @Operation(summary = "import document")
    @PostMapping("/import/{documentId}/{hcpId}/{language}")
    fun doImport(
        @PathVariable documentId: String,
        @PathVariable hcpId: String,
        @PathVariable language: String,
        @RequestParam protocolIds: String,
        @RequestParam formIds: String,
        @RequestParam planOfActionId: String,
        @RequestParam enckeys: String,
        @RequestBody ctc: ContactDto,
    ) = mono {
        documentLogic.getDocument(documentId)?.let {
            multiFormatLogic.doImport(
                language,
                it,
                hcpId,
                protocolIds.split(','),
                formIds.split(','),
                planOfActionId,
                contactMapper.map(ctc),
                if (isBlank(enckeys)) listOf() else enckeys.split(','),
            )?.let(contactMapper::map)
        }
    }
}
