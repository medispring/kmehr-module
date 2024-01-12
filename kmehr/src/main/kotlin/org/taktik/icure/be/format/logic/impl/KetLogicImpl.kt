/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.be.format.logic.impl

import kotlinx.coroutines.flow.flowOf
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.be.format.logic.KetLogic
import org.taktik.icure.domain.result.ResultInfo
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.IOException
import java.time.LocalDateTime
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

@Profile("kmehr")
@Service
class KetLogicImpl(
    healthcarePartyLogic: HealthcarePartyLogic,
    documentLogic: DocumentLogic
) : GenericResultFormatLogicImpl(healthcarePartyLogic, documentLogic), KetLogic {
    @Throws(IOException::class)
    override suspend fun canHandle(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): Boolean {
        return try {
            val xml = getXmlDocument(doc, encKeys, cachedAttachment)
            val xPathFactory = XPathFactory.newInstance()
            val xpath = xPathFactory.newXPath()
            val expr = xpath.compile("/Record/Header/LaboFileFormatVersion")
            (expr.evaluate(xml, XPathConstants.NODESET) as NodeList).length > 0
        } catch (e: ParserConfigurationException) {
            false
        } catch (e: SAXException) {
            false
        } catch (e: XPathExpressionException) {
            false
        }
    }

    @Throws(IOException::class)
    override suspend fun getInfos(doc: Document, full: Boolean, language: String, encKeys: List<String>, cachedAttachment: ByteArray?): List<ResultInfo> {
        return try {
            val xml = getXmlDocument(doc, encKeys, cachedAttachment)
            val xPathFactory = XPathFactory.newInstance()
            val xpath = xPathFactory.newXPath()
            val expr = xpath.compile("/Record/Body/Patient/Person")
            val nl = expr.evaluate(xml, XPathConstants.NODESET) as NodeList
            getStream(nl).map { n: Node -> getResultInfo(n) }.collect(Collectors.toList())
        } catch (e: ParserConfigurationException) {
            ArrayList()
        } catch (e: SAXException) {
            ArrayList()
        } catch (e: XPathExpressionException) {
            ArrayList()
        }
    }

    /**
     * Creates a Stream of Node from a NodeList by creating an iterator.
     * @param nl the NodeList.
     * @return a Stream of Node.
     */
    private fun getStream(nl: NodeList): Stream<Node> {
        return StreamSupport.stream(
            (
                Iterable {
                    object : Iterator<Node> {
                        var i = 0
                        override fun hasNext(): Boolean {
                            return i < nl.length
                        }

                        override fun next(): Node {
                            return nl.item(i++)
                        }
                    }
                }
                ).spliterator(),
            false,
        )
    }

    /**
     * Converts a XML Node to a ResultInfo.
     * @param n the Node.
     * @return a ResultInfo.
     */
    private fun getResultInfo(n: Node): ResultInfo {
        val resultInfo = ResultInfo()
        resultInfo.lastName = getStream(n.childNodes).filter { nd: Node -> (nd as Element).tagName == "LastName" }.findFirst().map { obj: Node -> obj.textContent }.orElse(null)
        resultInfo.firstName = getStream(n.childNodes).filter { nd: Node -> (nd as Element).tagName == "FirstName" }.findFirst().map { obj: Node -> obj.textContent }.orElse(null)
        return resultInfo
    }

    @Throws(IOException::class)
    override suspend fun doImport(language: String, doc: Document, hcpId: String?, protocolIds: List<String>, formIds: List<String>, planOfActionId: String?, ctc: Contact, encKeys: List<String>, cachedAttachment: ByteArray?): Contact? {
        return null
    }

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, text: String?) = flowOf<DataBuffer>()

}
