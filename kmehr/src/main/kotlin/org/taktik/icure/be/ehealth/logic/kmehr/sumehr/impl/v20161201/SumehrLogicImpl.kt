/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.sumehr.impl.v20161201

import be.fgov.ehealth.ehvalidator.core.EhValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.be.ehealth.dto.SumehrStatus
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.sumehr.SumehrLogic
import org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyTagCodeDateFilter
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
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteBuffer

@Profile("kmehr")
@org.springframework.stereotype.Service("sumehrLogicV2")
class SumehrLogicImpl(
    val contactLogic: ContactLogic,
    val healthcarePartyLogic: HealthcarePartyLogic,
    @Qualifier("sumehrExportV2") val sumehrExport: SumehrExport,
    @Qualifier("sumehrImportV2") val sumehrImport: SumehrImport
) : SumehrLogic {

    override suspend fun isSumehrValid(hcPartyId: String, patient: Patient, patientSecretForeignKeys: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean): SumehrStatus {

        val sumehrServiceIds = contactLogic.matchEntitiesBy(
            ServiceByHcPartyTagCodeDateFilter(
                healthcarePartyId = hcPartyId,
                patientSecretForeignKeys = patientSecretForeignKeys,
                tagType = "CD-TRANSACTION",
                tagCode = "sumehr"
            )
        ).toList()

        if (sumehrServiceIds.isEmpty()) {
            return SumehrStatus.absent
        }

        val comparator = Comparator<Service> { a, b -> a.modified?.compareTo(b.modified ?: 0) ?: -1 }

        val servicesByIds = contactLogic
            .getServices(sumehrServiceIds)
            .toList()
            .sortedWith(comparator)
            .associateBy { it.id }
            .filter { it.value.endOfLife == null }

        if (servicesByIds.isEmpty()) {
            return SumehrStatus.outdated
        }

        return if (servicesByIds.values.sortedWith(comparator).last().comment
            == getSumehrMd5(hcPartyId, patient, patientSecretForeignKeys, excludedIds, includeIrrelevantInformation))
            SumehrStatus.uptodate
        else SumehrStatus.outdated
    }

    override suspend fun getSumehrMd5(hcPartyId: String, patient: Patient, patientSecretForeignKeys: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean) =
        sumehrExport.getMd5(hcPartyId, patient, patientSecretForeignKeys, excludedIds, includeIrrelevantInformation)

    override suspend fun importSumehr(inputData: Flow<ByteBuffer>, author: User, language: String, dest: Patient?, mappings: Map<String, List<ImportMapping>>, saveToDatabase: Boolean): List<ImportResult> {
        return sumehrImport.importSumehr(inputData, author, language, mappings, saveToDatabase, dest)
    }

    override suspend fun importSumehrByItemId(inputData: Flow<ByteBuffer>, itemId: String, author: User, language: String, dest: Patient?, mappings: Map<String, List<ImportMapping>>, saveToDatabase: Boolean): List<ImportResult> {
        return sumehrImport.importSumehrByItemId(inputData, itemId, author, language, mappings, saveToDatabase, dest)
    }

    override fun createSumehr(
        pat: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        recipient: HealthcareParty,
        language: String,
        comment: String,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
        healthElements: List<HealthElement>?,
        config: Config,
    ) = sumehrExport.createSumehr(pat, sfks, sender, recipient, language, comment, excludedIds, includeIrrelevantInformation, decryptor, services, healthElements, config)

    @Throws(IOException::class)
    override fun validateSumehr(pat: Patient, sfks: List<String>, sender: HealthcareParty, recipient: HealthcareParty, language: String, comment: String, excludedIds: List<String>, includeIrrelevantInformation: Boolean, decryptor: AsyncDecrypt?, services: List<Service>?, healthElements: List<HealthElement>?, config: Config) = flow {
        val temp = File.createTempFile("temp", System.nanoTime().toString())

        val sos = sumehrExport.createSumehr(pat, sfks, sender, recipient, language, comment, excludedIds, includeIrrelevantInformation, decryptor, services, healthElements, config)
        try {
            val dataBuffer = sos.first()
            val html = EhValidator.getHTMLReport(temp.absolutePath, EhValidator.Language.french, "Sumehr")
            val w = OutputStreamWriter(dataBuffer.asOutputStream(), "UTF-8")
            w.write(html)
            w.close()
        } catch (e: SAXException) {
            throw IOException(e)
        }
        emitAll(sos)
    }.flowOn(Dispatchers.IO)

    override suspend fun getAllServices(hcPartyId: String, sfks: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean, decryptor: AsyncDecrypt?) =
        healthcarePartyLogic.getHealthcareParty(hcPartyId)?.let { healthcarePartyLogic.getHcpHierarchyIds(it) }?.let { sumehrExport.getAllServices(it, sfks, excludedIds, includeIrrelevantInformation, decryptor) } ?: listOf()

    override suspend fun getHealthElements(hcPartyId: String, sfks: List<String>, excludedIds: List<String>, includeIrrelevantInformation: Boolean): List<HealthElement> {
        return healthcarePartyLogic.getHealthcareParty(hcPartyId)?.let { healthcarePartyLogic.getHcpHierarchyIds(it) }?.let { sumehrExport.getHealthElements(it, sfks, excludedIds, includeIrrelevantInformation).second } ?: listOf()
    }

    override suspend fun getContactPeople(excludedIds: List<String>, patientId: String): List<Partnership> =
        sumehrExport.getContactPeople(excludedIds, patientId)

    override suspend fun getPatientHealthcareParties(excludedIds: List<String>, patientId: String): List<PatientHealthCareParty> =
        sumehrExport.getPatientHealthCareParties(excludedIds, patientId)
}
