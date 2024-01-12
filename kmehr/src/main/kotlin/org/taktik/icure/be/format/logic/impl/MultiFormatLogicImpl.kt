/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.be.format.logic.impl

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.format.logic.MultiFormatLogic
import org.taktik.icure.be.format.logic.ResultFormatLogic
import org.taktik.icure.domain.result.ResultInfo
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import java.io.IOException
import java.time.LocalDateTime
import java.util.function.Consumer

@Profile("kmehr")
@Service
class MultiFormatLogicImpl(
    private val engines: List<ResultFormatLogic>,
    private val documentLogic: DocumentLogic
) : MultiFormatLogic {
    @Throws(IOException::class)
    override suspend fun canHandle(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): Boolean {
        val attachment = documentLogic.getAndDecryptMainAttachment(doc.id, encKeys)
        return engines.firstOrNull { it.canHandle(doc, encKeys, attachment) } != null
    }

    @Throws(IOException::class)
    override suspend fun getInfos(doc: Document, full: Boolean, language: String, encKeys: List<String>, cachedAttachment: ByteArray?): List<ResultInfo> {
        val attachment = documentLogic.getAndDecryptMainAttachment(doc.id, encKeys)
        return engines.firstNotNullOfOrNull { e ->
            if (e.canHandle(doc, encKeys, attachment)) {
                val infos = e.getInfos(doc, full, language, encKeys, attachment)
                infos.forEach(Consumer { i: ResultInfo? -> i!!.engine = e.javaClass.name })
                infos
            } else null
        } ?: throw IllegalArgumentException("Invalid format")

    }

    @Throws(IOException::class)
    override suspend fun doImport(language: String, doc: Document, hcpId: String?, protocolIds: List<String>, formIds: List<String>, planOfActionId: String?, ctc: Contact, encKeys: List<String>, cachedAttachment: ByteArray?): Contact {
        val attachment = documentLogic.getAndDecryptMainAttachment(doc.id, encKeys)
        return engines.firstNotNullOfOrNull { e ->
            if (e.canHandle(doc, encKeys, attachment)) {
                e.doImport(language, doc, hcpId, protocolIds, formIds, planOfActionId, ctc, encKeys, attachment)
            } else null
        } ?: throw IllegalArgumentException("Invalid format")
    }

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, text: String?): Flow<DataBuffer> {
        throw UnsupportedOperationException()
    }

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, mimeType: String?, content: ByteArray?): Flow<DataBuffer> {
        throw UnsupportedOperationException()
    }
}
