/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.be.format.logic.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils.lowerCase
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.format.logic.HealthOneLogic
import org.taktik.icure.domain.result.ResultInfo
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.embed.AddressType
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.entities.embed.ReferenceRange
import org.taktik.icure.utils.FuzzyValues
import java.io.*
import java.nio.charset.UnsupportedCharsetException
import java.sql.Timestamp
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.regex.Pattern

@Profile("kmehr")
@Service
class HealthOneLogicImpl(
    healthcarePartyLogic: HealthcarePartyLogic,
    documentLogic: DocumentLogic,
    val patientLogic: PatientLogic,
    val contactLogic: ContactLogic
) : GenericResultFormatLogicImpl(healthcarePartyLogic, documentLogic), HealthOneLogic {
    private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")

    override suspend fun doImport(language: String, doc: Document, hcpId: String?, protocolIds: List<String>, formIds: List<String>, planOfActionId: String?, ctc: Contact, encKeys: List<String>, cachedAttachment: ByteArray?): Contact? {
        val text = decodeRawData(cachedAttachment ?: documentLogic.getAndDecryptMainAttachment(doc.id, encKeys))
        return if (text != null) {
            val r = StringReader(text)
            val lls = parseReportsAndLabs(language, protocolIds, r)
            val subContactsWithServices = fillContactWithLines(lls, planOfActionId, hcpId, protocolIds, formIds)
            val updatedContact = ctc.copy(subContacts = ctc.subContacts + subContactsWithServices.map { it.first }, services = ctc.services + subContactsWithServices.flatMap { it.second })
            if(ctc.rev != null) {
                contactLogic.modifyEntities(listOf(updatedContact)).firstOrNull()
                    ?: throw IllegalArgumentException("Cannot update contact ${ctc.id}, check the revision or your access to it")
            } else {
                if (updatedContact.delegations.isEmpty() && updatedContact.securityMetadata?.secureDelegations.isNullOrEmpty()) {
                    throw IllegalArgumentException("Cannot create a Contact with no delegation")
                }
                contactLogic.createContact(updatedContact)
            }
        } else {
            throw UnsupportedCharsetException("Charset could not be detected")
        }
    }

    /**
     * Reads a laboratory attachment from the provided reader, creating a List of LaboLines. Each of them contains the
     * parsed results and protocols as Services.
     * @param language the language of the Content of the Services in the LaboLines.
     * @param protocols
     * @param r the attachment reader.
     * @return a List of LaboLine.
     */
    @Throws(IOException::class)
    fun parseReportsAndLabs(language: String, protocols: List<String?>, r: Reader): List<LaboLine> {
        val result = mutableListOf<LaboLine>()
        var line: String? = null
        val reader = BufferedReader(r)
        var ll: LaboLine? = null
        var position: Long = 0
        while ((reader.readLine()?.also { line = it }) != null && position < 10000000L /* ultimate safeguard */) {
            position++
            if (isLaboLine(line!!)) {
                ll?.let { createServices(it, language, position) }
                ll = getLaboLine(line!!)
                if (protocols.contains(ll.resultReference) || protocols.size == 1 && protocols[0] != null && protocols[0]!!.startsWith("***")) {
                    result.add(ll)
                } else {
                    ll = null
                }
            } else if (ll != null && isLaboResultLine(line!!)) {
                val lrl = getLaboResultLine(line!!, ll)
                if (lrl != null) {
                    ll.isResultLabResult = true
                    if (ll.labosList.size > 0 && !(lrl.analysisCode == ll.labosList[0]!!.analysisCode && lrl.analysisType == ll.labosList[0]!!.analysisType)) {
                        createServices(ll, language, position)
                    }
                    ll.labosList.add(lrl)
                }
            } else if (ll != null && isProtocolLine(line!!)) {
                val pl = getProtocolLine(line!!)
                if (pl != null) { // Less than 20 lines ... If the codes are different, we probably have a bad header... Just concatenate
                    if (ll.protoList.size > 20 && pl.code != ll.protoList[ll.protoList.size - 1]!!.code) {
                        createServices(ll, language, position)
                    }
                    ll.protoList.add(pl)
                }
            } else if (ll != null && isResultsInfosLine(line!!)) {
                ll.ril = getResultsInfosLine(line!!)
            } else if (ll != null && isPatientAddressLine(line!!)) {
                ll.pal = getPatientAddressLine(line!!)
            }
        }
        ll?.let { createServices(it, language, position) }
        return result
    }

    /**
     * Creates a new iCure service for each lab result and protocol already registered in the LaboLine.
     * After that, it will remove all the results in [LaboLine.labosList] and [LaboLine.protoList]
     *
     * @param ll the [LaboLine]. It will be modified by this function.
     * @param language the language of the Content in the Services.
     * @param position the index of line in the document to parse, it will become the index of the service.
     */
    private fun createServices(ll: LaboLine, language: String, position: Long) {
        if (ll.labosList.size > 0 && ll.ril != null) {
            ll.services.addAll(importLaboResult(language, ll.labosList, position, ll.ril!!))
            ll.labosList.clear()
        }
        if (ll.protoList.size > 0 && ll.ril != null) {
            ll.services.add(importProtocol(language, ll.protoList, position, ll.ril!!))
            ll.protoList.clear()
        }
    }

    /**
     * Creates a Service from a List of ProtocolLine. The Service will have a String Content with a concatenation of all
     * the lines.
     * @param language the language of the Content.
     * @param protoList a List.
     * @param position the index of the Service.
     * @param ril a ResultInfoLine.
     * @return an iCureService.
     */
    private fun importProtocol(language: String, protoList: List<*>, position: Long, ril: ResultsInfosLine): org.taktik.icure.entities.embed.Service {
        val text = (1 until protoList.size).fold(((protoList[0] as ProtocolLine).text ?: "")) { acc, i ->
            acc + "\n" + ((protoList[i] as ProtocolLine).text ?: "")
        }
        return org.taktik.icure.entities.embed.Service(
            id = uuidGen.newGUID().toString(),
            content = mapOf(language to Content(stringValue = text)),
            label = "Protocol",
            index = position,
            valueDate = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(ril.demandDate, ZoneId.systemDefault()), ChronoUnit.DAYS),
        )
    }

    /**
     * Imports a laboratory result as a List of iCure Service. If the value is numeric, then it tries to parse it and
     * add the reference value. If it is not numeric or it is not able to parse it, then it will create a service with
     * a String Content.
     * @param language the language of the Content.
     * @param labResults a List.
     * @param position the index of the Service.
     * @param ril a ResultsInfosLine.
     * @return a List of Service.
     */
    private fun importLaboResult(language: String, labResults: List<*>, position: Long, ril: ResultsInfosLine): List<org.taktik.icure.entities.embed.Service> =
        if (labResults.size > 1) {
            var lrl = labResults[0] as LaboResultLine
            val laboResultLineValue = requireNotNull(lrl.value) {
                "First line of lab result cannot be null"
            }.replace("<".toRegex(), "").replace(">".toRegex(), "")
            if (tryToGetValueAsNumber(laboResultLineValue) != null) {
                val lrl2 = labResults[1] as LaboResultLine
                val comment = (2 until labResults.size).fold((lrl2.value ?: "")) { acc, i ->
                    lrl = labResults[i] as LaboResultLine
                    if (lrl.value?.isNotEmpty() == true) {
                        acc + "\n" + lrl.value
                    } else acc
                }
                addLaboResult(labResults[0] as LaboResultLine, language, position, ril, comment)
            } else {
                val label = lrl.analysisType
                val value = (2 until labResults.size).fold((lrl.value ?: "")) { acc, i ->
                    lrl = labResults[i] as LaboResultLine
                    if (lrl.value?.isNotEmpty() == true) {
                        acc + "\n" + lrl.value
                    } else acc
                }
                listOf(
                    org.taktik.icure.entities.embed.Service(
                        id = uuidGen.newGUID().toString(),
                        content = mapOf(language to Content(stringValue = value.takeIf { it.isNotBlank() })),
                        label = label ?: "",
                        index = position,
                        valueDate = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(ril.demandDate, ZoneId.systemDefault()), ChronoUnit.DAYS),
                    )
                )
            }
        } else {
            addLaboResult(labResults[0] as LaboResultLine, language, position, ril, null)
        }


    /**
     * Creates a new Service for a laboratory result. If it manages to parse the value as a number, than the Content will
     * be numeric otherwise will be a String.
     * @param lrl a LaboResultLine.
     * @param language the language of the Content.
     * @param position the index of the Service.
     * @param ril a ResultsInfoLine.
     * @param comment a comment to add to the Content if the value is numeric.
     * @return a List of Service containing the Service.
     */
    private fun addLaboResult(lrl: LaboResultLine, language: String, position: Long, ril: ResultsInfosLine, comment: String?): List<org.taktik.icure.entities.embed.Service> {
        val laboResultLineValue = lrl.value!!.replace("<".toRegex(), "").replace(">".toRegex(), "")
        val d = tryToGetValueAsNumber(laboResultLineValue)
        return if (d != null) { // We import as a Measure
            listOf(importNumericLaboResult(language, d, lrl, position, ril, comment))
        } else {
            listOf(importPlainStringLaboResult(language, lrl, position, ril))
        }
    }

    /**
     * Creates a new iCure Service with a string Content that has the severity of the result. It also adds the CD-SEVERITY
     * code to the service.
     * @param language the language of the Content.
     * @param lrl a LaboResultLine that contains the severity.
     * @param position the index of the Service.
     * @param ril a ResultInfosLine that contains the date for the Service.
     * @return an iCure Service.
     */
    private fun importPlainStringLaboResult(language: String, lrl: LaboResultLine, position: Long, ril: ResultsInfosLine): org.taktik.icure.entities.embed.Service {
        val referenceValue = lrl.referenceValues!!.trim { it <= ' ' }
        val severity = lrl.severity?.trim { it <= ' ' }
        val value = "${lrl.value} ${lrl.unit}" + (if (referenceValue.isNotEmpty()) {
                " (${lrl.referenceValues} )"
            } else "") + (
            if (severity?.isNotEmpty() == true) {
                " ($severity )"
            } else "" )

        return org.taktik.icure.entities.embed.Service(
            id = uuidGen.newGUID().toString(),
            content = mapOf(language to Content(stringValue = value)),
            label = lrl.analysisType ?: "",
            index = position,
            valueDate = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(ril.demandDate, ZoneId.systemDefault()), ChronoUnit.DAYS),
            codes = setOf(CodeStub.from("CD-SEVERITY", "abnormal", "1")).takeIf { severity?.isNotEmpty() == true } ?: setOf()
        )
    }

    /**
     * Creates a new iCure Service from a [LaboResultLine] with a numeric value.
     *
     * @param language the language of the Content.
     * @param d the numeric value.
     * @param lrl the [LaboResultLine].
     * @param position the index of the Service.
     * @param ril the [ResultsInfosLine].
     * @param comment a comment to add to the Content.
     * @return an iCure Service.
     */
    private fun importNumericLaboResult(language: String, d: Double?, lrl: LaboResultLine, position: Long, ril: ResultsInfosLine, comment: String?): org.taktik.icure.entities.embed.Service {
        val r = lrl.referenceValues?.let { tryToGetReferenceValues(humanLanguageReplacer(it)) }
        val severity = lrl.severity?.trim { it <= ' ' }
        return org.taktik.icure.entities.embed.Service(
            codes = setOf(CodeStub.from("CD-SEVERITY", "abnormal", "1")).takeIf { severity?.isNotEmpty() == true } ?: setOf(),
            id = uuidGen.newGUID().toString(),
            content = mapOf(
                language to Content(
                    measureValue = Measure(
                        value = d,
                        comment = comment,
                        unit = lrl.unit?.let { it.ifBlank { r?.unit } } ?: r?.unit,
                        referenceRanges = listOf(
                            ReferenceRange(
                                low = r?.minValue,
                                high = r?.maxValue
                            )
                        ).takeIf { r?.minValue != null || r?.maxValue != null } ?: emptyList(),
                        severity = severity?.takeIf { it.isNotBlank() }?.let {
                            when (it) {
                                "+++" -> 3
                                "++" -> 2
                                "+" -> 1
                                "(+)" -> 0
                                "N" -> 0
                                "(-)" -> 0
                                "-" -> -1
                                "--" -> -2
                                "---" -> -3
                                else -> 1
                            }
                        } ?: 0,
                        severityCode = severity,
                        sign = lrl.sign?.takeIf { it.isNotBlank() },
                        evolution = severity?.takeIf { it.isNotBlank() }?.let {
                            when (it) {
                                "(+)" -> 1
                                "N" -> 0
                                "(-)" -> -1
                                else -> null
                            }
                        },
                    ),
                ),
            ),
            label = lrl.analysisType ?: "",
            index = position,
            valueDate = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(ril.demandDate, ZoneId.systemDefault()), ChronoUnit.DAYS)
        )
    }

    protected fun humanLanguageReplacer(refValues: String): String{
        val lessThanStrings = listOf("moins de", "minder dan", "less than", "kleiner dan", "lager dan", "lower than")
        val greaterThanStrings = listOf("plus de", "au moins", "minstens", "tenminste", "meer dan", "groter dan", "larger than", "greater than", "hoger dan", "higher than")
        val separatorStrings = listOf(" ", ":", "-")
        var cleanedString = lowerCase(refValues)
        lessThanStrings.forEach{cleanedString = cleanedString.replace(it, "<")}
        greaterThanStrings.forEach{cleanedString = cleanedString.replace(it, ">")}
        if(cleanedString !== lowerCase(refValues)) { //only remove separator if there were string replacements
            separatorStrings.forEach {cleanedString = cleanedString.replace(it, "") }
            return cleanedString
        }
        return refValues
    }

    /**
     * Converts a String to a Double, if it is possible.
     * @param value the String
     * @return a Double if the conversion succeeded, null otherwise.
     */
    private fun tryToGetValueAsNumber(value: String?): Double? =
        value?.let {
            try {
                it.replace(",".toRegex(), ".").toDouble()
            } catch (e: Exception) { // System.out.println("--------- Failed to parse '" + numberS + "'");
                null
            }
        }

    /**
     * Tries to get the reference value from a String.
     * @param refValues the refValues.
     * @return a Reference, if found.
     */
    private fun tryToGetReferenceValues(refValues: String): Reference? {
        try {
            var m = betweenReference.matcher(refValues)
            if (m.matches()) {
                val r = Reference()
                r.minValue = m.group(1).replace(",".toRegex(), ".").toDouble()
                r.maxValue = m.group(2).replace(",".toRegex(), ".").toDouble()
                if (m.group(3) != null) {
                    r.unit = m.group(3)
                }
                if (m.group(4) != null) {
                    r.unit = m.group(4)
                }
                return r
            }
            m = lessThanReference.matcher(refValues)
            if (m.matches()) {
                val r = Reference()
                r.maxValue = m.group(1).replace(",".toRegex(), ".").toDouble()
                if (m.group(2) != null) {
                    r.unit = m.group(2)
                }
                if (m.group(3) != null) {
                    r.unit = m.group(3)
                }
                return r
            }
            m = greaterThanReference.matcher(refValues)
            if (m.matches()) {
                val r = Reference()
                r.minValue = m.group(1).replace(",".toRegex(), ".").toDouble()
                if (m.group(2) != null) {
                    r.unit = m.group(2)
                }
                if (m.group(3) != null) {
                    r.unit = m.group(3)
                }
                return r
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    @Throws(IOException::class)
    override suspend fun getInfos(doc: Document, full: Boolean, language: String, encKeys: List<String>, cachedAttachment: ByteArray?): List<ResultInfo> {
        val br = getBufferedReader(doc, encKeys, cachedAttachment) ?: throw IllegalArgumentException("Cannot get document")
        val documentId = doc.id
        return extractResultInfos(br, language, documentId, full)
    }

    /**
     * Given the reader for a document, it parses all the lines and creates a ResultInfo for each line.
     * @param br the BufferedReader of the document.
     * @param language the language of the Content of the Services that will be created in the ResultInfo.
     * @param documentId the id of the iCure Document origin of the attachment.
     * @param full
     * @return a List of ResultInfo.
     */
    @Throws(IOException::class)
    private fun extractResultInfos(br: BufferedReader, language: String, documentId: String?, full: Boolean): List<ResultInfo> {
        val l = mutableListOf<ResultInfo>()
        var position: Long = 0
        var line = br.readLine()
        while (line != null && position < 10000000L /* ultimate safeguard */) {
            position++
            if (isLaboLine(line)) {
                val ll = getLaboLine(line)
                val ri = ResultInfo(
                    labo = ll.labo,
                )
                line = br.readLine()
                while (line != null && position < 10000000L /* ultimate safeguard */) {
                    position++
                    when {
                        isPatientLine(line) -> {
                            val p = getPatientLine(line)
                            ri.lastName = p.lastName
                            ri.firstName = p.firstName
                            if (p.dn != null) {
                                ri.dateOfBirth = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(p.dn!!.time), ZoneId.systemDefault()), ChronoUnit.DAYS)
                            }
                            ri.protocol = p.protocol
                            ri.sex = p.sex
                            ri.documentId = documentId
                        }
                        isExtraPatientLine(line) -> {
                            val p = getExtraPatientLine(line)
                            if (p.dn != null) {
                                ri.dateOfBirth = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(p.dn!!.time), ZoneId.systemDefault()), ChronoUnit.DAYS)
                            }
                            if (p.sex != null) {
                                ri.sex = p.sex
                            }
                        }
                        isResultsInfosLine(line) -> {
                            val r = getResultsInfosLine(line)
                            ll.ril = r
                            if (r != null) {
                                ri.complete = r.isComplete
                                ri.demandDate = r.demandDate!!.toEpochMilli()
                            }
                        }
                        isPatientSSINLine(line) -> {
                            val p = getPatientSSINLine(line)
                            if (p != null) {
                                ri.ssin = p.ssin
                            }
                        }
                        isProtocolLine(line) -> {
                            if (ri.codes.isEmpty()) {
                                ri.codes.add(CodeStub.from("CD-TRANSACTION", "report", "1"))
                            }
                            if (full) {
                                val lrl = getProtocolLine(line)
                                if (lrl != null) {
                                    if (ll.protoList.size > 20 && lrl.code != ll.protoList[ll.protoList.size - 1]!!.code) {
                                        createServices(ll, language, position)
                                    }
                                    ll.protoList.add(lrl)
                                }
                            }
                        }
                        isLaboResultLine(line) -> {
                            if (ri.codes.isEmpty()) {
                                ri.codes.add(CodeStub.from("CD-TRANSACTION", "labresult", "1"))
                            }
                            if (full) {
                                val lrl = getLaboResultLine(line, ll)
                                if (lrl != null) {
                                    if (ll.labosList.size > 0 && !(lrl.analysisCode == ll.labosList[0]!!.analysisCode && lrl.analysisType == ll.labosList[0]!!.analysisType)) {
                                        createServices(ll, language, position)
                                    }
                                    ll.labosList.add(lrl)
                                }
                            }
                        }
                        isLaboLine(line) -> {
                            break
                        }
                    }
                    line = br.readLine()
                }
                if (full) {
                    createServices(ll, language, position)
                    ri.services = ll.services
                }
                if (ri.protocol == null || ri.protocol?.length == 0) {
                    ri.protocol = "***" + ri.demandDate
                }
                l.add(ri)
            } else {
                line = br.readLine()
            }
        }
        br.close()
        return l
    }

    private fun isPatientLine(line: String): Boolean =
        line.startsWith("A2") || line.matches(Regex(headerPattern + "S2.*"))

    private fun isExtraPatientLine(line: String): Boolean =
        line.matches(Regex(headerPattern + "S4.*"))

    private fun isPatientAddressLine(line: String): Boolean =
        line.startsWith("A3") || line.matches(Regex(headerPattern + "S3.*"))

    private fun isResultsInfosLine(line: String): Boolean =
        line.startsWith("A4") || line.matches(Regex(headerPattern + "S5.*"))

    private fun isPatientSSINLine(line: String): Boolean =
        line.startsWith("A5")

    private fun isLaboLine(line: String): Boolean =
        line.startsWith("A1") || line.matches(Regex(headerPattern + "S1.*"))

    private fun isLaboResultLine(line: String): Boolean =
        line.startsWith("L1") || line.matches(Regex(headerPattern + "R1.*"))

    private fun isProtocolLine(line: String): Boolean =
        line.startsWith("L5") || line.startsWith("L2")

    /**
     * Creates a LaboLine from a String.
     * @param line the String.
     * @return a LaboLine.
     */
    private fun getLaboLine(line: String): LaboLine {
        val parts = splitLine(line)
        val ll = LaboLine()
        if (parts.size > 1) {
            ll.resultReference = parts[1].trim { it <= ' ' }
        }
        if (parts.size > 2) {
            ll.labo = parts[2].trim { it <= ' ' }
        }
        ll.fullLine = line
        return ll
    }

    /**
     * Creates a PatientLine from a String.
     * @param line the String.
     * @return a PatientLine.
     */
    private fun getPatientLine(line: String): PatientLine {
        val parts = splitLine(line)
        val pl = PatientLine()
        if (parts.size > 1) {
            pl.protocol = parts[1].trim { it <= ' ' }
        }
        if (parts.size > 3) {
            pl.firstName = parts[3].trim { it <= ' ' }
        }
        if (parts.size > 2) {
            pl.lastName = parts[2].trim { it <= ' ' }
        }
        if (parts.size > 4) {
            pl.sex = if (parts[4].trim { it <= ' ' } == "V") "F" else parts[4].trim { it <= ' ' }
            if (parts.size > 5) {
                pl.dn = parseBirthDate(parts[5].trim { it <= ' ' })
            }
        }
        return pl
    }

    /**
     * Creates an additional PatientLine from a String.
     * @param line the String.
     * @return a PatientLine.
     */
    private fun getExtraPatientLine(line: String): PatientLine {
        val parts = splitLine(line)
        val pl = PatientLine()
        if (parts.size > 1) {
            pl.protocol = parts[1]
        }
        if (parts.size > 3) {
            pl.sex = if (parts[3].trim { it <= ' ' } == "V") "F" else parts[3].trim { it <= ' ' }
        }
        if (parts.size > 2) {
            pl.dn = parseBirthDate(parts[2].trim { it <= ' ' })
        }
        return pl
    }

    /**
     * Creates a [LaboResultLine] from a raw line of the document.
     *
     * @param line the raw line.
     * @param ll the [LaboLine] parsed up to now.
     * @return a [LaboResultLine] or null if the line is not in the correct format.
     */
    private fun getLaboResultLine(line: String, ll: LaboLine): LaboResultLine? =
        try {
            val parts = splitLine(line)
            val lrl = LaboResultLine()
            lrl.value = ""
            lrl.severity = lrl.value
            lrl.unit = lrl.severity
            lrl.referenceValues = lrl.unit
            lrl.analysisType = lrl.referenceValues
            lrl.analysisCode = lrl.analysisType
            lrl.protocol = lrl.analysisCode
            if (parts.size > 1) {
                lrl.protocol = parts[1].trim { it <= ' ' }
            }
            if (parts.size > 2) {
                lrl.analysisCode = parts[2].trim { it <= ' ' }
            }
            if (parts.size > 3) {
                lrl.analysisType = parts[3].trim { it <= ' ' }
            }
            if (!line.startsWith("L1")) {
                if (parts.size > 5) {
                    lrl.referenceValues = parts[4].trim { it <= ' ' } + " - " + parts[5].trim { it <= ' ' }
                }
                if (parts.size > 6) {
                    lrl.unit = parts[6].trim { it <= ' ' }
                }
                lrl.severity = ""
            } else {
                if (lrl.analysisType!!.isEmpty() && ll.labosList.size > 0 && ll.labosList[ll.labosList.size - 1]!!.analysisCode != null && ll.labosList[ll.labosList.size - 1]!!.analysisCode == lrl.analysisCode) {
                    lrl.analysisType = ll.labosList[ll.labosList.size - 1]!!.analysisType
                    lrl.value = parts[4].trim { it <= ' ' }
                } else {
                    if (parts.size > 4) {
                        lrl.referenceValues = parts[4].trim { it <= ' ' }
                    }
                    if (parts.size > 5) {
                        lrl.unit = parts[5].trim { it <= ' ' }
                    }
                    if (parts.size > 6) {
                        lrl.severity = parts[6].trim { it <= ' ' }
                    }
                }
            }
            if (lrl.value == "" && parts.size > 7) {
                lrl.value = parts[7].trim { it <= ' ' }
                lrl.sign = parts[7].trim().let { if (it.startsWith("<") || it.startsWith(">")) it.substring(0, 1) else null }
            }
            if (lrl.analysisType == null || lrl.analysisType == "") {
                lrl.analysisType = "untitled"
            }
            lrl
        } catch (e: Exception) {
            println("------------Line = $line")
            e.printStackTrace()
            null
        }

    /**
     * Creates a ProtocolLine from a String.
     * @param line the String.
     * @return a ProtocolLine.
     */
    private fun getProtocolLine(line: String): ProtocolLine? =
        try {
            val parts = splitLine(line)
            val pl = ProtocolLine()
            if (parts.size > 1) {
                pl.protocol = parts[1].trim { it <= ' ' }
            }
            if (parts.size > 2) {
                pl.code = parts[2].trim { it <= ' ' }
            }
            if (parts.size > 7) {
                pl.text = parts[7].trim { it <= ' ' }
            } else if (parts.size > 3) {
                pl.text = parts[3].trim { it <= ' ' }
            }
            pl
        } catch (e: Exception) {
            println("------------Line = $line")
            e.printStackTrace()
            null
        }

    /**
     * Creates a ResultsInfosLine from a String.
     * @param line the String.
     * @return a ResultsInfosLine.
     */
    private fun getResultsInfosLine(line: String): ResultsInfosLine? =
        try {
            val parts = splitLine(line)
            val ril = ResultsInfosLine()
            if (parts.size > 1) {
                ril.protocol = parts[1].trim { it <= ' ' }
            }
            ril.isComplete = parts.size <= 5 || parts[5].lowercase().contains("c")
            if (parts.size > 3) {
                ril.demandDate = parseDemandDate(parts[3].trim { it <= ' ' })
            }
            ril
        } catch (e: Exception) {
            println("------------Line = $line")
            e.printStackTrace()
            null
        }

    /**
     * Creates a PatientSSINLine from a String.
     * @param line the String.
     * @return a PatientSSINLine.
     */
    private fun getPatientSSINLine(line: String): PatientSSINLine? =
        try {
            val parts = splitLine(line)
            val psl = PatientSSINLine()
            if (parts.size > 1) {
                psl.protocol = parts[1]
            }
            if (parts.size > 3 && FuzzyValues.isSsin(parts[3])) {
                psl.ssin = parts[3]
            }
            if (parts.size > 4 && FuzzyValues.isSsin(parts[4])) {
                psl.ssin = parts[4]
            }
            psl
        } catch (e: Exception) {
            println("------------Line = $line")
            e.printStackTrace()
            null
        }

    /**
     * Creates a PatientAddressLine from a String.
     * @param line the String.
     * @return a PatienPatientAddressLinetSSINLine.
     */
    private fun getPatientAddressLine(line: String): PatientAddressLine {
        val parts = splitLine(line)
        val pal = PatientAddressLine()
        if (parts.size > 1) {
            pal.protocol = parts[1].trim { it <= ' ' }
        }
        if (parts.size > 4) {
            pal.locality = parts[4].trim { it <= ' ' }
        }
        if (parts.size > 3) {
            val zipMatcher = zipCode.matcher(parts[3].trim { it <= ' ' })
            if (zipMatcher.matches()) {
                pal.zipCode = zipMatcher.group(1)
            }
        }
        if (parts.size > 2) {
            val addressMatcher = address.matcher(parts[2].trim { it <= ' ' })
            if (addressMatcher.matches()) {
                pal.address = if (addressMatcher.group(1) == null) addressMatcher.group(3) else addressMatcher.group(2)
                pal.number = if (addressMatcher.group(1) == null) addressMatcher.group(4) else addressMatcher.group(1)
            } else {
                pal.address = parts[2].trim { it <= ' ' }
            }
        }
        return pal
    }

    private fun String.removeNewLineAndCarriageReturn(): String = replace("\n".toRegex(), "").replace("\r".toRegex(), "")

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, text: String?): Flow<DataBuffer> {
        val pw: PrintWriter
        val os = ByteArrayOutputStream(10000)

        pw = try {
            PrintWriter(OutputStreamWriter(os, "UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
        val namePat = patient!!.lastName?.removeNewLineAndCarriageReturn() ?: ""
        val firstPat = patient.firstName?.removeNewLineAndCarriageReturn() ?: ""
        val sexPat = patient.gender?.code?.removeNewLineAndCarriageReturn() ?: ""
        val birthPat = if (patient.dateOfBirth != null)
                patient.dateOfBirth.toString().replace("(....)(..)(..)".toRegex(), "$3$2$1")
                    .removeNewLineAndCarriageReturn()
            else ""
        val ssinPat = patient.ssin?.removeNewLineAndCarriageReturn() ?: ""
        val a = patient.addresses.stream().filter { ad -> ad.addressType == AddressType.home }.findFirst()
        val addrPat3 = a.map { obj -> obj.city?.removeNewLineAndCarriageReturn() }.orElse("")
        val addrPat2 = a.map { obj -> obj.postalCode?.removeNewLineAndCarriageReturn() }.orElse("")
        val addrPat1 = a.map { obj -> obj.street?.removeNewLineAndCarriageReturn() }.orElse("")
        val inamiMed = if (sender!!.nihii != null) sender.nihii?.removeNewLineAndCarriageReturn() else ""
        val nameMed = sender.lastName?.removeNewLineAndCarriageReturn() ?: ""
        val firstMed = sender.firstName?.removeNewLineAndCarriageReturn() ?: ""
        val dateAnal = if (date != null) date.format(shortDateTimeFormatter)?.removeNewLineAndCarriageReturn() else ""
        val isFull = "C"
        pw.print("A1\\$ref\\$inamiMed $nameMed $firstMed\\\r\n")
        pw.print("A2\\$ref\\$namePat\\$firstPat\\$sexPat\\$birthPat\\\r\n")
        pw.print("A3\\$ref\\$addrPat1\\$addrPat2\\$addrPat3\\\r\n")
        pw.print("A4\\$ref\\$inamiMed $nameMed $firstMed\\$dateAnal\\\\$isFull\\\r\n")
        pw.print("A5\\$ref\\\\$ssinPat\\\\\\\\\r\n")
        for (line in text!!.replace("\u2028".toRegex(), "\n").split("\n").toTypedArray()) {
            pw.print("L5\\$ref\\DIVER\\\\\\\\\\$line\\\r\n")
        }
        pw.flush()

        return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
    }

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, mimeType: String?, content: ByteArray?) = flowOf<DataBuffer>()

    private fun splitLine(line: String): Array<String> {
        val m = headerCompiledPrefix.matcher(line)
        return if (m.matches()) {
            val l: MutableList<String> = ArrayList()
            l.add(m.group(2))
            l.add(m.group(1))
            l.addAll(listOf(*m.group(3).split("\\\\".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
            l.toTypedArray()
        } else {
            line.split("\\\\".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
    }

    @Throws(IOException::class)
    override suspend fun canHandle(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): Boolean = withContext(Dispatchers.IO) {
        val br = getBufferedReader(doc, encKeys, cachedAttachment)
        val firstLine = br!!.readLine()
        br.close()
        firstLine != null && isLaboLine(firstLine)
    }

    /**
     * Converts a Date in a String format to a Timestamp. It performs different parsing according to the date format.
     * @param date the String date.
     * @return a timestamp.
     */
    @Throws(ParseException::class, NumberFormatException::class)
    private fun parseDate(date: String): Long =
        when (date.length) {
            8 -> shortDateFormat.parse(date.trim { it <= ' ' }).time
            6 -> shorterDateFormat.parse(date).time
            10 -> extraDateFormat.parse(date).time
            else -> throw NumberFormatException("Unreadable date: \"$date\"")
        }

    /**
     * Tries to parse a date in a String format, and returns null if it fails.
     * @param date the date String.
     * @return a Timestamp.
     */
    private fun parseBirthDate(date: String): Timestamp? {
        try {
            val d = parseDate(date)
            if (d > parseDate("01011800")) {
                return Timestamp(d)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Tries to parse a date in a String format, and returns the current timestamp if it fails.
     * @param date the date String.
     * @return an Instant.
     */
    private fun parseDemandDate(date: String): Instant {
        try {
            val d = parseDate(date)
            if (d > parseDate("01011800")) {
                return Instant.ofEpochMilli(d)
            }
        } catch (e: ParseException) {
            log.error("Date {} could not be parsed", date)
        } catch (e: NumberFormatException) {
            log.error("Date {} could not be parsed", date)
        }
        return Instant.now()
    }

    companion object {
        private val log = LoggerFactory.getLogger(HealthOneLogicImpl::class.java)
        val shorterDateFormat = SimpleDateFormat("ddMMyy")
        val shortDateFormat = SimpleDateFormat("ddMMyyyy")
        val extraDateFormat = SimpleDateFormat("dd/MM/yyyy")

        // \s*>\s*((?:-|\+)?[0-9]*(?:\.|,)?[0-9]*) matches __>__-01.29 and >+2,245 and >1  into $1
// (?:(?:\s*([^0-9\s]\S*))|(?:\s+(\S+)))?\s* matches a0eraa and __a5656 (first part) or (_898989) in other words: any garbage that is separed by a space or
// an alphanumerical character
// We also allow for an open parenthesis, an open [ or both
        val greaterThanReference: Pattern = Pattern.compile("\\s*(?:[\\(\\[]+\\s*)?>\\s*((?:-|\\+)?[0-9]*(?:\\.|,)?[0-9]*)(?:(?:\\s*([^0-9\\s]\\S*))|(?:\\s+(\\S+)))?\\s*")

        // The same with <
        val lessThanReference: Pattern = Pattern.compile("\\s*(?:[\\(\\[]+\\s*)?<\\s*((?:-|\\+)?[0-9]*(?:\\.|,)?[0-9]*)(?:(?:\\s*([^0-9\\s]\\S*))|(?:\\s+(\\S+)))?\\s*")

        // GROUPA = ((?:-|\+)?[0-9]*(?:\.|,)?[0-9]*)\s* matches -01.29 and +2,245 and 1  into $1
// We match _GROUPA__-__GROUPA[GARBAGE]
// We also allow for an open parenthesis
        val betweenReference: Pattern = Pattern.compile("\\s*(?:[\\(\\[]+\\s*)?((?:-|\\+)?[0-9]*(?:\\.|,)?[0-9]*)\\s*[-:]\\s*((?:-|\\+)?[0-9]*(?:\\.|,)?[0-9]*)(?:(?:\\s*([^0-9\\s]\\S*))|(?:\\s+(\\S+)))?\\s*")
        val address: Pattern = Pattern.compile("^(?:\\s*(\\d+)(?:\\s*,\\s*|\\s+)(\\S.*?\\S)\\s*)|(?:\\s*(\\S.*?\\S)(?:\\s*,\\s*|\\s+)(\\d+)\\s*)$")
        val zipCode: Pattern = Pattern.compile("^\\s*(\\d+)\\s*$")
        const val headerPattern: String = "^\\s*(\\d+)\\s+"
        val headerCompiledPrefix: Pattern = Pattern.compile("^\\s*[0-9][0-9][0-9][0-9](\\d+)\\s+([A-Z][0-9])(.*)$")
    }
}
