/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.be.format.logic.impl

import com.google.common.base.Strings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.format.logic.MedidocLogic
import org.taktik.icure.domain.result.ResultInfo
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.embed.*
import org.taktik.icure.utils.FuzzyValues
import java.io.*
import java.text.DateFormat
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
class MedidocLogicImpl(
    healthcarePartyLogic: HealthcarePartyLogic,
    documentLogic: DocumentLogic,
    val contactLogic: ContactLogic
) : GenericResultFormatLogicImpl(healthcarePartyLogic, documentLogic), MedidocLogic {
    private val p1 = Pattern.compile("^#A.*$")
    private val p2 = Pattern.compile("^#R[a-zA-Z]*\\s*$")
    private val p3 = Pattern.compile("^#A/\\s*$")
    private val p4 = Pattern.compile("^#R/\\s*$")
    private val p5 = Pattern.compile("^#/[0-9]*\\s*$")
    private val df: DateFormat = SimpleDateFormat("yyyyMMdd")
    private val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val idf: DateFormat = SimpleDateFormat("ddMMyyyy")
    private val sidf: DateFormat = SimpleDateFormat("ddMMyy")
    private val onlyNumbersAndPercentSigns = Pattern.compile("^[0-9%]+$")

    override suspend fun canHandle(doc: Document, encKeys: List<String>, cachedAttachment: ByteArray?): Boolean {
        var hasAHash = false
        var hasAHashSlash = false
        var hasRHash = false
        var hasRHashSlash = false
        var hasFinalTag = false
        val text = decodeRawData(cachedAttachment ?: documentLogic.getAndDecryptMainAttachment(doc.id, encKeys))
        if (text != null) {
            val reader = BufferedReader(StringReader(text))
            while (reader.readLine()?.also { line ->
                    if (p1.matcher(line).matches()) {
                        hasAHash = true
                    }
                    if (p2.matcher(line).matches()) {
                        hasRHash = true
                    }
                    if (p3.matcher(line).matches() && hasAHash) {
                        hasAHashSlash = true
                    }
                    if (p4.matcher(line).matches() && hasRHash) {
                        hasRHashSlash = true
                    }
                    if (p5.matcher(line).matches()) {
                        hasFinalTag = true
                    }
                } != null
            ) { }
        }
        return hasAHash && hasAHashSlash && hasRHash && hasRHashSlash && hasFinalTag
    }

    @Throws(IOException::class)
    override suspend fun getInfos(doc: Document, full: Boolean, language: String, encKeys: List<String>, cachedAttachment: ByteArray?): List<ResultInfo> {
        val l = mutableListOf<ResultInfo>()
        val br = getBufferedReader(doc, encKeys, cachedAttachment)
        val lines = IOUtils.readLines(br)
        val labo = lines[1].replace("  +".toRegex(), " ")
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (p1.matcher(line).matches() && !p3.matcher(line).matches() && i + 9 < lines.size) {
                val ri = ResultInfo()
                ri.codes.add(CodeStub.from("CD-TRANSACTION", "report", "1"))
                ri.labo = labo
                ri.documentId = doc.id
                ri.lastName = lines[i + 1].substring(0, 24).trim { it <= ' ' }
                ri.firstName = lines[i + 1].substring(24).trim { it <= ' ' }
                var birthDateLine = lines[i + 2].trim { it <= ' ' }
                // The examples we got do NOT respect the format at all
// There seems to be one common variant where address and NISS
// come before the birth date.
                val isStandardFormat = onlyNumbersAndPercentSigns.matcher(lines[i + 2].trim { it <= ' ' }).matches()
                if (!isStandardFormat) {
                    birthDateLine = lines[i + 5].trim { it <= ' ' }
                }
                try {
                    ri.dateOfBirth = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(parseDate(birthDateLine)!!.time), ZoneId.systemDefault()), ChronoUnit.DAYS)
                } catch (ignored: ParseException) {
                } catch (ignored: NullPointerException) {
                }
                val gender = lines[if (isStandardFormat) i + 3 else i + 6].trim { it <= ' ' }.uppercase()
                if (gender == "X" || gender == "Y") {
                    ri.sex = if (gender == "X") "F" else "M"
                }
                val demandDate = getResultDate(lines, i, isStandardFormat)
                if (demandDate != null) {
                    ri.demandDate = demandDate.time
                }
                val code = getProtocolCode(lines, i, isStandardFormat, demandDate)
                ri.protocol = code
                i += if (isStandardFormat) 6 else 9
                if (full) {
                    val (ii, s) = fillService(language, lines, i, demandDate)
                    i = ii
                    ri.services = listOf(s)
                }
                l.add(ri)
            }
            i++
        }
        return l
    }

    /**
     * Creates a protocol code from a List of String.
     * @param lines the List of String.
     * @param i the index of the starting element.
     * @param isStandardFormat
     * @param demandDate
     * @return the protocol code as String.
     */
    private fun getProtocolCode(lines: List<String>, i: Int, isStandardFormat: Boolean, demandDate: Date?): String? =
        if (isStandardFormat) {
            try {
                val date = parseDate(lines[i + 2].trim { it <= ' ' })
                computeProtocolCode(
                    lines[i + 1].substring(0, 24.coerceAtMost(lines[i + 1].length)).trim { it <= ' ' },
                    if (lines[i + 1].length > 24) lines[i + 1].substring(24).trim { it <= ' ' } else "",
                    date?.time ?: System.currentTimeMillis(),
                    demandDate!!.time,
                    lines[i + 5],
                )
            } catch (e: ParseException) {
                e.printStackTrace()
                null
            } catch (e: NullPointerException) {
                e.printStackTrace()
                null
            }
        } else {
            try {
                computeProtocolCode(
                    lines[i + 1].substring(0, 24).trim { it <= ' ' },
                    lines[i + 1].substring(24).trim { it <= ' ' },
                    parseDate(lines[i + 5].trim { it <= ' ' })!!.time,
                    demandDate!!.time,
                    lines[i + 8],
                )
            } catch (e: ParseException) {
                e.printStackTrace()
                null
            } catch (e: NullPointerException) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Tries to parse a date from a String of a List, returns null if it is not possible.
     * @param lines a List of String.
     * @param i the index of the String to use.
     * @param isStandardFormat if it is true, it uses the i+4th String, otherwise uses the 1+7th
     * @return a Date or null.
     */
    private fun getResultDate(lines: List<String>, i: Int, isStandardFormat: Boolean): Date? =
        if (isStandardFormat) {
            try {
                parseDate(lines[i + 4].trim { it <= ' ' })
            } catch (ignored: ParseException) {
                null
            }
        } else {
            try {
                parseDate(lines[i + 7].trim { it <= ' ' })
            } catch (ignored: ParseException) {
                null
            }
        }

    @Throws(IOException::class)
    override suspend fun doImport(language: String, doc: Document, hcpId: String?, protocolIds: List<String>, formIds: List<String>, planOfActionId: String?, ctc: Contact, encKeys: List<String>, cachedAttachment: ByteArray?): Contact? {
        val br = getBufferedReader(doc, encKeys, cachedAttachment)
        val lines = IOUtils.readLines(br)
        val lls = mutableListOf<LaboLine?>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (p1.matcher(line).matches() && !p3.matcher(line).matches() && i + 9 < lines.size) { // The examples we got do NOT respect the format at all
// There seems to be one common variant where address and NISS
// come before the birth date.
                val isStandardFormat = onlyNumbersAndPercentSigns.matcher(lines[i + 2].trim { it <= ' ' }).matches()
                val demandDate = getResultDate(lines, i, isStandardFormat)
                val code = getProtocolCode(lines, i, isStandardFormat, demandDate)
                i += if (isStandardFormat) 6 else 9
                if (protocolIds.contains(code) || protocolIds.size == 1 && protocolIds[0].startsWith("***")) {
                    val (ii, s) = fillService(language, lines, i, demandDate)
                    i = ii
                    val labo = lines[1].replace("  +".toRegex(), " ")
                    val ll = LaboLine()
                    lls.add(ll)
                    ll.services = mutableListOf(s)
                    ll.resultReference = code
                    ll.labo = labo
                }
            }
            i++
        }
        val subContactsWithServices = fillContactWithLines(lls.filterNotNull(), planOfActionId, hcpId, protocolIds, formIds)
        val updatedContact = ctc.copy(subContacts = ctc.subContacts + subContactsWithServices.map { it.first }, services = ctc.services + subContactsWithServices.flatMap { it.second })
        return if(updatedContact.rev != null) {
            contactLogic.modifyEntities(listOf(updatedContact)).firstOrNull()
                ?: throw IllegalArgumentException("Cannot update contact ${ctc.id}, check the revision or your access to it")
        } else {
            if (updatedContact.delegations.isEmpty() && updatedContact.securityMetadata?.secureDelegations.isNullOrEmpty()) {
                throw IllegalArgumentException("Cannot create a Contact with no delegation")
            }
            contactLogic.createContact(updatedContact)
        }
    }

    /**
     * Creates a Service from the string provided.
     * @param language the language fo the Service content.
     * @param lines the List of String.
     * @param i where to start in the lines List.
     * @param demandDate if not null, it will be set as valueDate in the Service. Otherwise, now will be used.
     * @return a Pair where the first element is an index and the second a Service.
     */
    private fun fillService(language: String, lines: List<String>, i: Int, demandDate: Date?): Pair<Int, org.taktik.icure.entities.embed.Service> {
        var i = i
        do {
            i++
        } while (!p2.matcher(lines[i]).matches())
        // Skip p2 and first empty line
        i += 2
        val b = StringBuilder()
        while (!p4.matcher(lines[i]).matches()) {
            b.append(lines[i]).append("\n")
            i++
        }
        return i to Service(
            id = uuidGen.newGUID().toString(),
            content = mapOf(language to Content(stringValue = b.toString())),
            label = "Protocol",
            valueDate = FuzzyValues.getFuzzyDate(LocalDateTime.ofInstant(if (demandDate != null) Instant.ofEpochMilli(demandDate.time) else Instant.now(), ZoneId.systemDefault()), ChronoUnit.DAYS),
        )
    }

    override fun doExport(sender: HealthcareParty?, recipient: HealthcareParty?, patient: Patient?, date: LocalDateTime?, ref: String?, text: String?): Flow<DataBuffer> {
        val pw: PrintWriter
        val os = ByteArrayOutputStream(10000)
        pw = try {
            PrintWriter(OutputStreamWriter(os, "UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
        if (sender!!.nihii != null && recipient!!.nihii != null) { // 1
            pw.print(
                sender.nihii!!.replace(
                    "([0-9])([0-9]{5})([0-9][0-9])([0-9][0-9][0-9])".toRegex(),
                    "$1/$2/$3/$4",
                ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            // 2
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(sender.lastName, 0, 24), 24) +
                        StringUtils.rightPad(StringUtils.substring(if (sender.firstName == null) "" else sender.firstName, 0, 16), 16)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            val senderAddress = patient!!.addresses.firstOrNull { ad: Address -> ad.addressType == AddressType.work }
                ?: patient.addresses.firstOrNull { ad: Address -> ad.addressType == AddressType.clinic }
                ?: patient.addresses.firstOrNull { ad: Address -> ad.addressType == AddressType.hospital }
                ?: patient.addresses.firstOrNull { ad: Address -> ad.addressType == AddressType.hq }
                ?: patient.addresses.firstOrNull { ad: Address -> ad.addressType == AddressType.other }
                ?: patient.addresses.firstOrNull { ad: Address -> ad.addressType == AddressType.home }

            // 3
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(senderAddress?.let { obj: Address -> obj.street } ?: "", 0, 35), 35) +
                        StringUtils.rightPad(StringUtils.substring(senderAddress?.let { obj: Address -> obj.houseNumber } ?: "", 0, 10), 10)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            // 4
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(senderAddress?.let { obj: Address -> obj.postalCode } ?: "", 0, 10), 10) +
                        StringUtils.rightPad(StringUtils.substring(senderAddress?.let { obj: Address -> obj.city } ?: "", 0, 35), 35)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            val senderTelecoms = senderAddress?.let { obj: Address -> obj.telecoms } ?: LinkedList()
            var senderPhone = senderTelecoms.stream().filter { t: Telecom -> t.telecomType == TelecomType.phone }.findFirst()
            if (!senderPhone.isPresent) {
                senderPhone = senderTelecoms.stream().filter { t: Telecom -> t.telecomType == TelecomType.mobile }.findFirst()
            }
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(senderPhone.map { obj: Telecom -> obj.telecomNumber }.orElse(""), 0, 25), 25) +
                        StringUtils.rightPad(StringUtils.substring(senderPhone.map { obj: Telecom -> obj.telecomNumber }.orElse(""), 0, 25), 25)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            // 6
            pw.print("\r\n")
            // 7
            pw.print(df.format(Date()) + "\r\n")
            // 8
            pw.print(
                recipient.nihii!!.replace(
                    "([0-9])([0-9]{5})([0-9][0-9])([0-9][0-9][0-9])".toRegex(),
                    "$1/$2/$3/$4",
                ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            // 9
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(recipient.lastName, 0, 24), 24) +
                        StringUtils.rightPad(StringUtils.substring(if (recipient.firstName == null) "" else recipient.firstName, 0, 16), 16)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            pw.print("#A" + (if (Strings.isNullOrEmpty(patient.ssin)) "" else patient.ssin) + "\r\n")
            // 2
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(patient.lastName, 0, 24), 24) +
                        StringUtils.rightPad(StringUtils.substring(if (patient.firstName == null) "" else patient.firstName, 0, 16), 16)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            // 3
            pw.print(patient.dateOfBirth.toString() + "\r\n")
            // 4
            pw.print((if (patient.gender == null) "Z" else if (patient.gender!!.code == "F") "X" else "Y") + "\r\n")
            pw.print(date!!.format(dtf) + "\r\n")
            pw.print(ref!!.replace("-".toRegex(), "").substring(0, 14) + "\r\n")
            pw.print("C\r\n")
            val patientAddress = patient.addresses.stream().filter { ad: Address -> ad.addressType == AddressType.home }.findFirst()
            pw.print(
                (
                    StringUtils.rightPad(StringUtils.substring(patientAddress.map { obj: Address -> obj.street }.orElse(""), 0, 24), 24) +
                        StringUtils.rightPad(StringUtils.substring(patientAddress.map { obj: Address -> obj.houseNumber }.orElse(""), 0, 7), 7)
                    ).replace("[\\r\\n]".toRegex(), "") + "\r\n",
            )
            pw.print(StringUtils.rightPad(StringUtils.substring(patientAddress.map { obj: Address -> obj.postalCode }.orElse(""), 0, 7), 7).replace("[\\r\\n]".toRegex(), ""))
            pw.print(StringUtils.rightPad(StringUtils.substring(patientAddress.map { obj: Address -> obj.city }.orElse(""), 0, 24), 24).replace("[\\r\\n]".toRegex(), "") + "\r\n")
            pw.print("#Rb\r\n")
            pw.print("!Protocole\r\n")
            pw.print("\r\n")
            pw.print(text!!.replace("\u2028".toRegex(), "\n").replace("\n".toRegex(), "\r\n") + "\r\n")
            pw.print("#R/\r\n")
            pw.print("#A/\r\n")
            pw.print("#/\r\n")
        }
        pw.flush()
        return DataBufferUtils.read(ByteArrayResource(os.toByteArray()), DefaultDataBufferFactory(), 10000).asFlow()
    }

    /**
     * Concatenates the data provided as parameter to obtain a protocol code.
     * @param name
     * @param first
     * @param birth
     * @param req
     * @param code
     * @return the ProtocolCode.
     */
    private fun computeProtocolCode(name: String, first: String, birth: Long, req: Long, code: String): String =
        "" + StringUtils.substring(name.replace(" ".toRegex(), ""), 0, 16) +
            StringUtils.substring(first.replace(" ".toRegex(), ""), 0, 8) +
            (birth / (1000 * 3600 * 24)).toInt() +
            (req / (1000 * 3600 * 24)).toInt() +
            StringUtils.substring(code, 0, 20)

    /**
     * Tries to parse a Date from a String. Returns null if not possible.
     * @param dateString the date as String.
     * @return a Date or null.
     */
    @Throws(ParseException::class)
    private fun parseDate(dateString: String): Date? {
        if (dateString.contains("%")) {
            return null
        }
        if (dateString.startsWith("0000")) {
            return null
        }
        if (dateString.length < 6) {
            return null
        }
        if (dateString.length < 8) {
            return if (dateString.substring(4, 6).toInt() > 31) {
                sidf.parse(dateString)
            } else {
                if (dateString.substring(0, 2).toInt() < 18) {
                    df.parse("20$dateString")
                } else {
                    df.parse("19$dateString")
                }
            }
        }
        return if (dateString.substring(4, 8).toInt() > 1300) { // Last digits are a year. Let's guess a ddMMyyyy
            idf.parse(dateString)
        } else { // You won't believe it... It follows the doc
            df.parse(dateString)
        }
    }
}
