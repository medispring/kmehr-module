/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.wscontrollers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.be.ehealth.dto.kmehr.v20110701.Utils.makeXGC
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.diarynote.DiaryNoteLogic
import org.taktik.icure.be.ehealth.logic.kmehr.medicationscheme.MedicationSchemeLogic
import org.taktik.icure.be.ehealth.logic.kmehr.smf.SoftwareMedicalFileLogic
import org.taktik.icure.be.ehealth.logic.kmehr.sumehr.SumehrLogic
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.services.external.http.WsController
import org.taktik.icure.services.external.http.websocket.annotation.WSOperation
import org.taktik.icure.services.external.http.websocket.annotation.WSParam
import org.taktik.icure.services.external.http.websocket.annotation.WSRequestMapping
import org.taktik.icure.services.external.http.websocket.operation.KmehrFileOperation
import org.taktik.icure.services.external.rest.v2.dto.be.kmehr.DiaryNoteExportInfoDto
import org.taktik.icure.services.external.rest.v2.dto.be.kmehr.MedicationSchemeExportInfoDto
import org.taktik.icure.services.external.rest.v2.dto.be.kmehr.SoftwareMedicalFileExportDto
import org.taktik.icure.services.external.rest.v2.dto.be.kmehr.SumehrExportInfoDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthcarePartyV2Mapper
import reactor.core.publisher.Mono
import java.time.Instant

@Profile("kmehr")
@Component("kmehrWsControllerV2")
@WSRequestMapping("/ws/v2/be_kmehr")
class KmehrWsController(
    private val sessionLogic: SessionInformationProvider,
    @Qualifier("sumehrLogicV1") val sumehrLogicV1: SumehrLogic,
    @Qualifier("sumehrLogicV2") val sumehrLogicV2: SumehrLogic,
    private val diaryNoteLogic: DiaryNoteLogic,
    private val softwareMedicalFileLogic: SoftwareMedicalFileLogic,
    private val medicationSchemeLogic: MedicationSchemeLogic,
    private val healthcarePartyLogic: HealthcarePartyLogic,
    private val patientLogic: PatientLogic,
    private val kmehrConfig: KmehrConfiguration,
    private val healthcarePartyV2Mapper: HealthcarePartyV2Mapper,
) : WsController {

    internal val log = LogFactory.getLog(KmehrWsController::class.java)

    @WSRequestMapping("/generateDiaryNote")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun generateDiaryNote(
        @WSParam("patientId") patientId: String,
        @WSParam("info") info: DiaryNoteExportInfoDto,
        operation: KmehrFileOperation
    ) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                healthcareParty?.let { it1 ->
                    operation.binaryResponse(
                        diaryNoteLogic.createDiaryNote(
                            it,
                            info.secretForeignKeys,
                            it1, healthcarePartyV2Mapper.map(info.recipient!!),
                            info.note,
                            info.tags,
                            info.contexts,
                            info.psy ?: false,
                            info.documentId,
                            info.attachmentId)
                    )
                }
            }
        } catch (e: Exception) {
            operation.errorResponse(e)
        }
    }

    @WSRequestMapping("/generateSumehr")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun generateSumehr(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: SumehrExportInfoDto,
        operation: KmehrFileOperation
    ) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                healthcareParty?.let { it1 ->
                    operation.binaryResponse(
                        sumehrLogicV1.createSumehr(
                            it, info.secretForeignKeys,
                            it1,
                            healthcarePartyV2Mapper.map(info.recipient!!),
                            language,
                            info.comment,
                            info.excludedIds,
                            info.includeIrrelevantInformation ?: false,
                            operation,
                            null,
                            null,
                            Config(
                                "" + System.currentTimeMillis(),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                Config.Software(info.softwareName ?: "iCure", info.softwareVersion ?: kmehrConfig.kmehrVersion),
                                "",
                                "en",
                                Config.Format.SUMEHR,
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                operation.errorResponse(e)
            }
        }
    }

    @WSRequestMapping("/validateSumehr")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun validateSumehr(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: SumehrExportInfoDto,
        operation: KmehrFileOperation
    ) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                healthcareParty?.let { it1 ->
                    operation.binaryResponse(
                        sumehrLogicV1.validateSumehr(
                            it,
                            info.secretForeignKeys,
                            it1,
                            healthcarePartyV2Mapper.map(info.recipient!!),
                            language,
                            info.comment,
                            info.excludedIds,
                            info.includeIrrelevantInformation ?: false,
                            operation,
                            null,
                            null,
                            Config(
                                "" + System.currentTimeMillis(),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                Config.Software(info.softwareName ?: "iCure", info.softwareVersion ?: kmehrConfig.kmehrVersion),
                                "",
                                "en",
                                Config.Format.SUMEHR,
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                operation.errorResponse(e)
            }
        }
    }

    @WSRequestMapping("/generateSumehrV2")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun generateSumehrV2(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: SumehrExportInfoDto,
        operation: KmehrFileOperation) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                healthcareParty?.let { it1 ->
                    operation.binaryResponse(
                        sumehrLogicV2.createSumehr(
                            it, info.secretForeignKeys,
                            it1,
                            healthcarePartyV2Mapper.map(info.recipient!!),
                            language,
                            info.comment,
                            info.excludedIds,
                            info.includeIrrelevantInformation ?: false,
                            operation,
                            null,
                            null,
                            Config(
                                "" + System.currentTimeMillis(),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                Config.Software(info.softwareName ?: "iCure", info.softwareVersion ?: kmehrConfig.kmehrVersion),
                                "",
                                "en",
                                Config.Format.SUMEHR
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                operation.errorResponse(e)
            }
        }
    }

    @WSRequestMapping("/generateSumehrV2JSON")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun generateSumehrV2JSON(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: SumehrExportInfoDto,
        operation: KmehrFileOperation
    ) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                healthcareParty?.let { it1 ->
                    operation.binaryResponse(
                        sumehrLogicV2.createSumehr(
                            it,
                            info.secretForeignKeys,
                            it1,
                            healthcarePartyV2Mapper.map(info.recipient!!),
                            language,
                            info.comment,
                            info.excludedIds,
                            info.includeIrrelevantInformation ?: false,
                            operation,
                            null,
                            null,
                            Config(
                                "" + System.currentTimeMillis(),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                Config.Software(info.softwareName ?: "iCure", info.softwareVersion ?: kmehrConfig.kmehrVersion),
                                "",
                                "en",
                                Config.Format.SUMEHR
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                operation.errorResponse(e)
            }
        }
    }

    @WSRequestMapping("/validateSumehrV2")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun validateSumehrV2(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: SumehrExportInfoDto,
        operation: KmehrFileOperation
    ) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                healthcareParty?.let { it1 ->
                    operation.binaryResponse(
                        sumehrLogicV2.validateSumehr(
                            it,
                            info.secretForeignKeys,
                            it1,
                            healthcarePartyV2Mapper.map(info.recipient!!),
                            language,
                            info.comment,
                            info.excludedIds,
                            info.includeIrrelevantInformation ?: false,
                            operation,
                            null,
                            null,
                            Config(
                                "" + System.currentTimeMillis(),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                makeXGC(Instant.now().toEpochMilli(), true),
                                Config.Software(info.softwareName ?: "iCure", info.softwareVersion ?: kmehrConfig.kmehrVersion),
                                "",
                                "en",
                                Config.Format.SUMEHR
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                operation.errorResponse(e)
            }
        }
    }

    @WSRequestMapping("/generateSmf")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun generateSmfExport(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: SoftwareMedicalFileExportDto,
        operation: KmehrFileOperation
    ): Mono<Unit> = mono {
            try {
                val patient = patientLogic.getPatient(patientId)
                val healthcareParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
                patient?.let { pat ->
                    healthcareParty?.let { hcp ->
                        try {
                            val smfExport = softwareMedicalFileLogic.createSmfExport(
                                pat,
                                info.secretForeignKeys,
                                hcp,
                                language,
                                operation,
                                operation,
                                Config(
                                    "" + System.currentTimeMillis(),
                                    makeXGC(Instant.now().toEpochMilli(), true),
                                    makeXGC(Instant.now().toEpochMilli(), true),
                                    Config.Software(
                                        info.softwareName ?: "iCure",
                                        info.softwareVersion
                                            ?: kmehrConfig.kmehrVersion,
                                    ),
                                    "",
                                    "en",
                                    if (info.exportAsPMF) Config.Format.PMF else Config.Format.SMF,
                                )
                            )
                            operation.binaryResponse(smfExport)
                        } catch (e: Exception) {
                            log.error("Cannot generate SMF", e)
                            operation.errorResponse(e)
                        }
                    }
                }
            } catch (e: Exception) {
                operation.errorResponse(e)
            }
        }

    @WSRequestMapping("/generateMedicationScheme")
    @WSOperation(adapterClass = KmehrFileOperation::class)
    fun generateMedicationSchemeExport(
        @WSParam("patientId") patientId: String,
        @WSParam("language") language: String,
        @WSParam("info") info: MedicationSchemeExportInfoDto,
        @WSParam("recipientSafe") recipientSafe: String,
        @WSParam("version") version: Int,
        operation: KmehrFileOperation
    ) = mono {
        try {
            val patient = patientLogic.getPatient(patientId)
            val hcParty = healthcarePartyLogic.getHealthcareParty(sessionLogic.getCurrentHealthcarePartyId())
            patient?.let {
                hcParty?.let { it1 ->
                    operation.binaryResponse(
                        medicationSchemeLogic.createMedicationSchemeExport(
                            it,
                            info.secretForeignKeys,
                            it1,
                            language,
                            recipientSafe,
                            version,
                            operation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                operation.errorResponse(e)
            }
        }
    }
}
