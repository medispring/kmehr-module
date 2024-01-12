/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.be.format.logic.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.format.logic.ResultFormatLogic
import org.taktik.icure.db.detectFrenchCp850Cp1252
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.ServiceLink
import org.taktik.icure.entities.embed.SubContact
import org.xml.sax.SAXException
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

abstract class GenericResultFormatLogicImpl(
    val healthcarePartyLogic: HealthcarePartyLogic,
    val documentLogic: DocumentLogic
) : ResultFormatLogic {
    protected var uuidGen = UUIDGenerator()

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, mimeType: String?, content: ByteArray?): Flow<DataBuffer> {
        throw UnsupportedOperationException("Not implemented")
    }

    /**
     * Given a list of LaboLine, creates a SubContact for each one of them.
     * @param lls the List of LaboLine.
     * @param planOfActionId the planOfActionId to add to each SubContact.
     * @param hcpId the id responsible for the SubContacts.
     * @param protocolIds
     * @param formIds
     * @return a List of Pairs where each first element is a SubContact and each second element is the corresponding
     * Services.
     */
    protected fun fillContactWithLines(
        lls: List<LaboLine>,
        planOfActionId: String?,
        hcpId: String?,
        protocolIds: List<String?>,
        formIds: List<String?>,
    ) =
        lls.map { ll ->
            val formId = protocolIds.last{
                (it == (ll.ril?.protocol ?: ll.resultReference)) || protocolIds.size == 1 && (it?.startsWith("***") == true)
            }?.let { formIds[protocolIds.lastIndexOf(it)] }
            SubContact(
                id = uuidGen.newGUID().toString(),
                responsible = hcpId,
                descr = ll.labo,
                protocol = ll.resultReference,
                planOfActionId = planOfActionId,
                status = (
                    (if (ll.isResultLabResult) SubContact.STATUS_LABO_RESULT else SubContact.STATUS_PROTOCOL_RESULT)
                        or SubContact.STATUS_UNREAD or if (ll.ril != null && ll.ril!!.isComplete) SubContact.STATUS_COMPLETE else 0
                    ),
                formId = formId,
                services = ll.services.map { s -> ServiceLink(s.id) }.toList(),
            ) to ll.services
        }

    /**
     * Decodes a ByteArray to String, adjusting the encoding if according to the french language characters used in it.
     * @param rawData the ByteArray.
     * @return the decoded String.
     */
    @Throws(IOException::class)
    protected fun decodeRawData(rawData: ByteArray?): String? =
        rawData?.let{
            // Test BOM
            // Test utf-16 byte order mark presence
            val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
            try {
                val decodedChars = utf8Decoder.decode(ByteBuffer.wrap(it))
                decodedChars.toString()
            } catch (e: CharacterCodingException) {
                val frenchCp850OrCp1252 = detectFrenchCp850Cp1252(it)
                val charset = if ("cp850" == frenchCp850OrCp1252) Charset.forName("cp850") else Charset.forName("cp1252")
                String(it, charset)
            }
        }

    /**
     * Loads the main attachment from the iCure Document passed as parameter and converts it to an XML Document.
     * @param doc the iCure Document that contains the attachment.
     * @param encKeys the encryption keys to decrypt the attachment.
     * @param cachedAttachment if not null, this is used instead of retrieving the main attachment
     * @return an XML Document.
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    protected suspend fun getXmlDocument(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): org.w3c.dom.Document = withContext(Dispatchers.IO){
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        dBuilder.parse(
            ByteArrayInputStream(
                cachedAttachment ?: documentLogic.getAndDecryptMainAttachment(doc.id, encKeys)
            )
        )
    }

    /**
     * Retrieves and tries to decrupt the main attachment of the Document passed as parameter.
     * @param doc the Document where to extract the Attachment.
     * @param encKeys the keys to decrypt the Attachment.
     * @param cachedAttachment if not null, this is used instead of retrieving the main attachment
     * @return a BufferedReader for the Attachment.
     */
    @Throws(IOException::class)
    protected suspend fun getBufferedReader(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): BufferedReader? {
        return decodeRawData(
            cachedAttachment ?: documentLogic.getAndDecryptMainAttachment(doc.id, encKeys)
        )?.let { BufferedReader(StringReader(it)) }
    }

    class LaboLine {
        var labo: String? = null
        var resultReference: String? = null
        var fullLine: String? = null
        var labosList: MutableList<LaboResultLine?> = ArrayList()
        var protoList: MutableList<ProtocolLine?> = ArrayList()
        var ril: ResultsInfosLine? = null
        var pal: PatientAddressLine? = null
        var services: MutableList<Service> = ArrayList()
        var isResultLabResult = false
    }

    inner class PatientLine {
        var lastName: String? = null
        var firstName: String? = null
        var dn: Timestamp? = null
        var sex: String? = null
        var protocol: String? = null
    }

    inner class PatientAddressLine {
        var protocol: String? = null
        var address: String? = null
        var number: String? = null
        var zipCode: String? = null
        var locality: String? = null
    }

    inner class PatientSSINLine {
        var protocol: String? = null
        var ssin: String? = null
    }

    inner class ProtocolLine {
        var protocol: String? = null
        var code: String? = null
        var text: String? = null
    }

    inner class LaboResultLine {
        var protocol: String? = null
        var analysisCode: String? = null
        var analysisType: String? = null
        var referenceValues: String? = null
        var unit: String? = null
        var severity: String? = null
        var value: String? = null
        var sign: String? = null
    }

    inner class ResultsInfosLine {
        var protocol: String? = null
        var demandDate: Instant? = null
        var isComplete = false
    }

    inner class Reference {
        var minValue: Double? = null
        var maxValue: Double? = null
        var unit: String? = null
    }
}
