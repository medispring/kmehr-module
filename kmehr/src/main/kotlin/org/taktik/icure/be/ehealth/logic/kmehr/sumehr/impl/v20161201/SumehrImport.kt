/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.sumehr.impl.v20161201

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.logic.kmehr.validSsinOrNull
import org.taktik.icure.db.equals
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.AddressType
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Duration
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.entities.embed.Medication
import org.taktik.icure.entities.embed.Medicinalproduct
import org.taktik.icure.entities.embed.RegimenItem
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.ServiceLink
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.entities.embed.Substanceproduct
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.entities.embed.TelecomType
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDADDRESSschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTYschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDSEXvalues
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTELECOMschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTYschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENTschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.AddressTypeBase
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.HeadingType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.ItemType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.PersonType
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.toInputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.time.temporal.ChronoUnit
import java.util.*
import javax.xml.bind.JAXBContext

@Profile("kmehr")
@org.springframework.stereotype.Service("sumehrImportV2")
class SumehrImport(
    val patientLogic: PatientLogic,
    val healthcarePartyLogic: HealthcarePartyLogic,
    val userLogic: UserLogic,
    val healthElementLogic: HealthElementLogic,
    val contactLogic: ContactLogic,
    val documentLogic: DocumentLogic,
    private val idGenerator: UUIDGenerator,
) {

    /**
     * Converts a SumEHR XML entities into a set of iCure entities. For each folder in the SumEHR, it creates a new
     * Contact with HealthElements or Services. If the HCP and Patients in the SumEHR do not exist, it creates them and
     * may save them to the database.
     * @param inputData the SumEHR XML as a flow of bytes
     * @param author the HCP User responsible for the import
     * @param mappings
     * @param saveToDatabase whether to save the new HCP and Patients to the database
     * @param dest a Patient to use instead of the ones contained in the SumEHR
     * @return a list of ImportResult, one for each folder in the SumEHR
     */
    suspend fun importSumehr(
        inputData: Flow<ByteBuffer>,
        author: User,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        dest: Patient? = null,
    ): List<ImportResult> {
        val jc = JAXBContext.newInstance(Kmehrmessage::class.java)
        val inputStream = inputData.toInputStream()
        val unmarshaller = jc.createUnmarshaller()
        val kmehrMessage = unmarshaller.unmarshal(inputStream) as Kmehrmessage

        val allRes = LinkedList<ImportResult>()

        // TODO Might want to have several implementations babsed on standards
        kmehrMessage.header.sender.hcparties?.forEach { createOrProcessHcp(it, saveToDatabase) }
        kmehrMessage.folders.forEach { folder ->
            val res = ImportResult().apply { allRes.add(this) }
            createOrProcessPatient(folder.patient, author, res, saveToDatabase, dest)?.let { patient ->
                res.patient = patient
                folder.transactions.forEach { trn ->
                    val ctc: Contact = when (trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value) {
                        "sumehr" -> parseSumehr(trn, author, res, language, mappings, saveToDatabase)
                        else -> parseGenericTransaction(trn, author, res, language, mappings, saveToDatabase)
                    }
                    if (saveToDatabase) {
                        contactLogic.createContact(ctc)
                    }
                    res.ctcs.add(ctc)
                }
            }
        }
        return allRes
    }

    /**
     * Converts a SumEHR XML entities into a set of iCure entities. For each folder in the SumEHR, it creates a new
     * Contact with HealthElements or Services. If the HCP and Patients in the SumEHR do not exist, it creates them and
     * may save them to the database.
     * @param inputData the SumEHR XML as a flow of bytes
     * @param itemId
     * @param author the HCP User responsible for the import
     * @param mappings
     * @param saveToDatabase whether to save the new HCP and Patients to the database
     * @param dest a Patient to use instead of the ones contained in the SumEHR
     * @return a list of ImportResult, one for each folder in the SumEHR
     */
    suspend fun importSumehrByItemId(
        inputData: Flow<ByteBuffer>,
        itemId: String,
        author: User,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        dest: Patient? = null,
    ): List<ImportResult> {
        val jc = JAXBContext.newInstance(Kmehrmessage::class.java)
        val inputStream = inputData.toInputStream()
        val unmarshaller = jc.createUnmarshaller()
        val kmehrMessage = unmarshaller.unmarshal(inputStream) as Kmehrmessage

        val allRes = LinkedList<ImportResult>()

        // TODO Might want to have several implementations babsed on standards
        kmehrMessage.header.sender.hcparties?.forEach { createOrProcessHcp(it, saveToDatabase) }
        kmehrMessage.folders.forEach { folder ->
            val res = ImportResult().apply { allRes.add(this) }
            createOrProcessPatient(folder.patient, author, res, saveToDatabase, dest)?.let { patient ->
                res.patient = patient
                folder.transactions.forEach { trn ->
                    val ctc: Contact = when (trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value) {
                        "sumehr" -> parseSumehr(trn, author, res, language, mappings, saveToDatabase, itemId)
                        else -> parseGenericTransaction(trn, author, res, language, mappings, saveToDatabase, itemId)
                    }
                    contactLogic.createContact(ctc)
                    res.ctcs.add(ctc)
                }
            }
        }
        return allRes
    }

    /**
     * Parses the SumEHR creating a new Contact.
     * @param trn the TransactionType element
     * @param author the HCP User responsible for the import
     * @param v the ImportResult
     * @param language the language for the content
     * @param mappings
     * @param saveToDatabase whether to add the non-existing Patients and HCP contained in the SumEHR to the database
     * @param itemId
     * @return a Contact
     */
    private suspend fun parseSumehr(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        itemId: String? = null,
    ): Contact = parseGenericTransaction(trn, author, v, language, mappings, saveToDatabase, itemId)


    /**
     * Creates a new contact by parsing a TransactionType from the SumEHR. It extracts the list of items from the
     * transaction and then creates a new Contact that contains all the items converted to HealthElements or
     * Services.
     * @param trn the TransactionType element
     * @param author the HCP User responsible for the import
     * @param v the ImportResult
     * @param language the language for the content
     * @param mappings
     * @param saveToDatabase whether to add the non-existing Patients and HCP contained in the SumEHR to the database
     * @param itemId
     * @return a Contact
     */
    private suspend fun parseGenericTransaction(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        itemId: String? = null,
    ): Contact {
        val contactId = idGenerator.newGUID().toString()
        val contactDate = trn.date?.let { Utils.makeFuzzyLongFromDateAndTime(it, trn.time) }
            ?: trn.findItem { it: ItemType -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encounterdatetime" } }?.let { itemType ->
                itemType.contents?.find { it.date != null }?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) }
            }
        val items = if (itemId?.isNotBlank() == true && itemId.isNotEmpty()) {
            // itemId = "[headingId].[itemId]" OR "[itemId]"
            val idList = itemId.split("/")
            if (idList.count() > 1) {
                // headings and items
                trn.findItemsByHeadingId(null, idList[0])
                    .filter { it.ids.count { id -> id.s == IDKMEHRschemes.ID_KMEHR && id.value == idList[1] } > 0 }
            } else {
                // only items
                trn.findItems().filter { it.ids.count { id -> id.s == IDKMEHRschemes.ID_KMEHR && id.value == idList[0] } > 0 }
            }
        } else {
            trn.findItems()
        }

        val (hes, svcs, sctcs) = items.fold(Triple(listOf<HealthElement>(), listOf<Service>(), listOf<SubContact>())) { (hes, svcs, sctcs), item ->
            val cdItem = item.cds.find { it.s == CDITEMschemes.CD_ITEM }?.value?.let { if (it == "problem") "healthcareelement" else it } ?: "note"
            val mapping =
                mappings[cdItem]?.find { (it.lifecycle == "*" || it.lifecycle == item.lifecycle?.cd?.value?.value()) && ((it.content == "*") || item.hasContentOfType(it.content)) }
            val label =
                item.cds.find { it.s == CDITEMschemes.LOCAL && it.sl == "iCureLabel" }?.value
                    ?: item.contents.filter { (it.texts?.size ?: 0) > 0 }
                        .flatMap { content ->
                            content.texts.filter {
                                it.l == language
                            }.map {
                                it.value
                            }
                        }.takeIf { it.isNotEmpty() }?.joinToString(" ")
                    ?: mapping?.label?.get(language)
                    ?: mappings["note"]?.lastOrNull()?.label?.get(language)
                    ?: "Note"

            when {
                (listOf("healthcareelement", "allergy", "adr", "risk", "socialrisk").contains(cdItem)) -> {
                     parseHealthcareElement(
                        mapping?.tags?.find { it.type == "CD-ITEM" }?.code ?: cdItem,
                        label,
                        item,
                        author,
                        language
                    ).let {
                        v.hes.add(
                            if (saveToDatabase) {
                                healthElementLogic.createEntities(listOf(it)).firstOrNull()
                                    ?: throw (IllegalStateException("Cannot save to database"))
                            } else {
                                it
                            },
                        )
                    }
                    Triple(hes, svcs, sctcs)
                }
                else -> {
                    parseGenericItem(
                        mapping?.tags?.find { it.type == "CD-ITEM" }?.code ?: cdItem,
                        label,
                        item,
                        author,
                        language)?.let {
                        Triple(
                            hes,
                            svcs + it,
                            sctcs + SubContact(id = idGenerator.newGUID().toString(), services = listOf(ServiceLink(it.id))))
                        } ?: Triple(hes, svcs, sctcs)
                }
            }
        }
        v.hes.addAll(
            if (saveToDatabase)
                hes.map { healthElementLogic.createEntities(listOf(it)).firstOrNull() ?: throw (IllegalStateException("Cannot save to database")) }
            else hes
        )

        return Contact(
            id = contactId,
            author = author.id,
            responsible = trn.author?.hcparties
                ?.filter { hcp -> hcp.cds.any { it.s == CDHCPARTYschemes.CD_HCPARTY && it.value == "persphysician" } }
                ?.firstNotNullOfOrNull { createOrProcessHcp(it, saveToDatabase) }?.id
                ?: author.healthcarePartyId,
            openingDate = contactDate,
            closingDate = trn.isIscomplete.let { if (it) contactDate else null },
            location = trn.findItem { it: ItemType -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encounterlocation" } }
                ?.let {
                    it.contents?.flatMap { content -> content.texts.map { text -> text.value } }?.joinToString(",")
                },
            tags = trn.findItem { it: ItemType -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encountertype" } }?.let {
                it.contents?.mapNotNull { content ->
                    content.cds?.find { cds -> cds.s == CDCONTENTschemes.CD_ENCOUNTER }
                        ?.value?.let { value -> CodeStub.from("CD-ENCOUNTER", value, "1.0") }
                }?.toSet()
            } ?: setOf(),
            services = svcs.toSet(),
            subContacts = sctcs.toSet(),
        )
    }

    /**
     * Creates a new HealthElement using the data extracted from the SumEHR
     * @param cdItem
     * @param label
     * @param item the parsed ItemType SumEHR response
     * @param author the HCP user responsible for the import
     * @param contactId the ID of the contact this HealthElement belongs to
     * @return the HealthElement
     */
    private fun parseHealthcareElement(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        contactId: String,
    ): HealthElement {
        val heDate = item.beginmoment?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) }
            ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)
        return HealthElement(
            id = idGenerator.newGUID().toString(),
            healthElementId = idGenerator.newGUID().toString(),
            descr = label,
            tags = setOf(CodeStub.from("CD-ITEM", cdItem, "1")) + extractTags(item).toSet() + (
                item.lifecycle?.let { setOf(CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1")) }
                    ?: setOf()
                ),
            author = author.id,
            responsible = author.healthcarePartyId,
            codes = extractCodes(item).toMutableSet(),
            valueDate = heDate,
            openingDate = heDate,
            closingDate = item.endmoment?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) },
            idOpeningContact = contactId,
            created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
            status = (item.lifecycle?.cd?.value?.value()
                    ?.let {
                        when {
                            (it == "inactive" || it == "aborted" || it == "canceled") -> 1
                            (it == "notpresent" || it == "excluded") -> 4
                            else -> 0
                        }
                    } ?: 0) + if (item.isIsrelevant != true) 2 else 0
        )
    }

    /**
     * Converts the content of an ItemType XML SumEHR to a collection of codes, but only if their type is
     * CD_DRUG_CNK, ICD, ICPC, CD_CLINICAL, CD_ATC, CD_PATIENTWILL, or CD_VACCINEINDICATION.
     * @param item an ItemType instance
     * @return the codes contained in the SumEHR
     */
    private fun extractCodes(item: ItemType): Set<CodeStub> =
        (item.cds
            .filter { it.s == CDITEMschemes.ICPC || it.s == CDITEMschemes.ICD }
            .map { CodeStub.from(it.s.value(), it.value, it.sv) } +
        item.contents.filter { (it.cds?.size ?: 0) > 0 }.flatMap {
            it.cds.filter { cdContent ->
                listOf(
                    CDCONTENTschemes.CD_DRUG_CNK,
                    CDCONTENTschemes.ICD,
                    CDCONTENTschemes.ICPC,
                    CDCONTENTschemes.CD_CLINICAL,
                    CDCONTENTschemes.CD_ATC,
                    CDCONTENTschemes.CD_PATIENTWILL,
                    CDCONTENTschemes.CD_VACCINEINDICATION,
                ).contains(cdContent.s)
            }.map { cdContent -> CodeStub.from(cdContent.s.value(), cdContent.value, cdContent.sv) }
        }).toSet()

    /**
     * Converts the content of an ItemType XML SumEHR to a collection of codes, but only if their type is
     * CD_PARAMETER, CD_LAB, or CD_TECHNICAL
     * @param item an ItemType instance
     * @return the codes contained in the SumEHR
     */
    private fun extractTags(item: ItemType): Collection<CodeStub> =
        (item.cds
            .filter { it.s == CDITEMschemes.CD_PARAMETER || it.s == CDITEMschemes.CD_LAB || it.s == CDITEMschemes.CD_TECHNICAL }
            .map { CodeStub.from(it.s.value(), it.value, it.sv) } +
        item.contents.filter { (it.cds?.size ?: 0) > 0 }.flatMap { content ->
            content.cds.filter {
                listOf(CDCONTENTschemes.CD_LAB).contains(it.s)
            }.map { CodeStub.from(it.s.value(), it.value, it.sv) }
        }).toSet()

    /**
     * Parses a ItemType from the SumEHR into a Service with a content.
     * @param cdItem
     * @param label
     * @param item the ItemType from the SumEHR
     * @param author the HCP user responsible for the import
     * @param language the language of the content
     */
    private fun parseGenericItem(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        language: String
    ): Service? {
        val svcDate = item.beginmoment?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) }
            ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)
        val content = when {
            (item.contents.any { it.substanceproduct != null || it.medicinalproduct != null || it.compoundprescription != null }) ->
                Content(
                    medicationValue = Medication(
                        substanceProduct = item.contents.firstOrNull { it.substanceproduct != null }?.let {
                            it.substanceproduct?.let { content ->
                                Substanceproduct(
                                    intendedcds = content.intendedcd?.let { cd -> listOf(CodeStub.from(cd.s.value(), cd.value, cd.sv)) }
                                        ?: listOf(),
                                    intendedname = content.intendedname.toString(),
                                )
                            }
                        },
                        medicinalProduct = item.contents.firstOrNull { it.medicinalproduct != null }?.let {
                            it.medicinalproduct?.let { medicinalProduct ->
                                Medicinalproduct(
                                    intendedcds = medicinalProduct.intendedcds?.map { cds -> CodeStub.from(cds.s.toString(), cds.value, cds.sv) }
                                        ?: listOf(),
                                    intendedname = medicinalProduct.intendedname.toString(),
                                )
                            }
                        },
                        compoundPrescription = item.contents.map {
                            // TODO: redo this
                            // var con: List<TextType> = it.compoundprescription?.content as List<String>
                            // con.map { it.value }.joinToString("")
                            ""
                        }.firstOrNull(),
                        instructionForPatient = listOfNotNull(
                            item.instructionforpatient?.value,
                            item.lnks.mapNotNull { it.value?.toString(Charsets.UTF_8) }.joinToString(", ")
                                .takeIf { it.isNotBlank() }).joinToString(" "),
                        regimen = item.regimen?.let {
                            it.daynumbersAndQuantitiesAndDates.map {
                                RegimenItem().apply {
                                    // TODO finish this optional parsing
                                }
                            }
                        },
                        duration = item.duration?.let { dt ->
                            Duration(
                                value = dt.decimal.toDouble(),
                                unit = dt.unit?.cd?.let { CodeStub.from(it.s.value(), it.value, it.sv) },
                            )
                        },
                        numberOfPackages = item.quantity?.decimal?.toInt(),
                        batch = item.batch,
                    ),
                )
            (item.contents.any { it.decimal != null }) -> item.contents.firstOrNull { it.decimal != null }?.let {
                if (it.unit != null) {
                    Content(measureValue = Measure(value = it.decimal.toDouble(), unit = it.unit?.cd?.value))
                } else {
                    Content(numberValue = it.decimal.toDouble())
                }
            }
            (item.contents.any { it.texts.any { text -> text.value?.isNotBlank() ?: false } }) -> {
                val textValue = item.contents
                    .filter { (it.texts?.size ?: 0) > 0 }
                    .flatMap { it.texts.map { text -> text.value } }
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
                if (cdItem == "CD-PARAMETER") {
                    // Try harder to convert to measure
                    item.contents.filter { (it.texts?.size ?: 0) > 0 }.flatMap { content ->
                        content.texts.map { text ->
                            text.value?.let {
                                val unit = it.replace(Regex("[0-9.,] *"), "")
                                val value = it.replace(Regex("([0-9.,]) *.*"), "$1")
                                try {
                                    Measure(
                                        value = value.toDouble(),
                                        unit = unit,
                                    )
                                } catch (ignored: NumberFormatException) {
                                    null
                                }
                            }
                        }
                    }.filterNotNull().firstOrNull()?.let { Content(measureValue = it) }
                        ?: Content(stringValue = textValue)
                } else {
                    Content(stringValue = textValue)
                }
            }
            (item.contents.any { it.isBoolean != null }) -> item.contents.firstOrNull { it.isBoolean != null }?.let {
                Content(booleanValue = it.isBoolean)
            }
            else -> null
        }
        return content?.let { c ->
            Service(
                id = idGenerator.newGUID().toString(),
                tags = setOf(CodeStub.from("CD-ITEM", cdItem, "1")) + extractTags(item).toSet() + (
                    item.lifecycle?.let { lifeCycle -> setOf(CodeStub.from("CD-LIFECYCLE", lifeCycle.cd.value.value(), "1")) }
                        ?: setOf()),
                codes = extractCodes(item).toMutableSet(),
                label = label,
                responsible = author.healthcarePartyId,
                valueDate = svcDate,
                openingDate = svcDate,
                closingDate = item.endmoment?.let { endMoment -> Utils.makeFuzzyLongFromDateAndTime(endMoment.date, endMoment.time) },
                created = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                modified = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli(),
                status = (item.lifecycle?.cd?.value?.value()?.let {
                        when {
                            (it == "inactive" || it == "aborted" || it == "canceled") -> 1
                            (it == "notpresent" || it == "excluded") -> 4
                            else -> 0
                        }
                    } ?: 0) + if (item.isIsrelevant != true) 2 else 0,
                content = mapOf(language to c),
            )
        }
    }

    private fun ItemType.hasContentOfType(content: String?): Boolean =
        content?.let { c ->
            c == "m" && this.contents.any { it.medicinalproduct != null || it.substanceproduct != null || it.compoundprescription != null } ||
                c == "s" && this.contents.any { (it.texts?.size ?: 0) > 0 || (it.cds?.size ?: 0) > 0 || it.hcparty != null }
        } ?: true

    /**
     * Finds a HCP from nihii or niss. If it does not exist, it creates it.
     * @param p a HcpartyType that contained in the SumEHR message
     * @param saveToDatabase whether saving the new HCP to the db or not
     * @return the retrieved or newly created HCP
     */
    private suspend fun createOrProcessHcp(p: HcpartyType, saveToDatabase: Boolean): HealthcareParty? {
        val nihii = p.ids.find { it.s == IDHCPARTYschemes.ID_HCPARTY }?.value
        val niss = p.ids.find { it.s == IDHCPARTYschemes.INSS }?.value

        return nihii
            ?.let {
                healthcarePartyLogic.listHealthcarePartiesByNihii(it).firstOrNull()
            } ?: niss?.let {
                healthcarePartyLogic.listHealthcarePartiesBySsin(niss).firstOrNull()
            } ?: try {
                    copyFromHcpToHcp(
                        p,
                        HealthcareParty(
                            id = idGenerator.newGUID().toString(),
                            nihii = nihii,
                            ssin = niss,
                        ),
                    ).let { if (saveToDatabase) healthcarePartyLogic.createHealthcareParty(it) else it }
                } catch (e: MissingRequirementsException) {
                    null
                }
    }

    /**
     * Creates a new HCP merging the information contained ina HcpartyType and a HCP.
     * @param p the HcpartyType
     * @param hcp the HealthcareParty
     * @return the new HealthcareParty
     */
    private fun copyFromHcpToHcp(p: HcpartyType, hcp: HealthcareParty): HealthcareParty =
        hcp.copy(
            firstName = hcp.firstName ?: p.firstname,
            lastName = hcp.lastName ?: p.familyname,
            name = hcp.name ?: p.name,
            ssin = hcp.ssin ?: p.ids.find { it.s == IDHCPARTYschemes.INSS }?.value,
            nihii = hcp.nihii ?: p.ids.find { it.s == IDHCPARTYschemes.ID_HCPARTY }?.value,
            speciality = hcp.speciality ?: p.cds.find { it.s == CDHCPARTYschemes.CD_HCPARTY }?.value,
            addresses = hcp.addresses + (
                p.addresses?.let { addresses ->
                    addresses.map { a ->
                        val addressType = a.cds
                            .find { it.s == CDADDRESSschemes.CD_ADDRESS }
                            ?.let { AddressType.valueOf(it.value) }
                        Address(
                            addressType = addressType,
                            street = a.street,
                            city = a.city,
                            houseNumber = a.housenumber,
                            postboxNumber = a.postboxnumber,
                            postalCode = a.zip,
                            country = a.country?.cd?.value,
                            telecoms = p.telecoms
                                .filter { t -> t.cds.find { it.s == CDTELECOMschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) } == addressType }
                                .mapNotNull { telecom ->
                                    telecom.cds.find { it.s == CDTELECOMschemes.CD_TELECOM }?.let { TelecomType.valueOf(it.value) }?.let { telecomType ->
                                        Telecom(telecomType = telecomType, telecomNumber = telecom.telecomnumber)
                                    }
                                },
                        )
                    }
                } ?: listOf()
                )
        )

    /**
     * Gets a patient using an HCP or one of his parents.
     * @param p the Patient data from the SumEHR
     * @param author the HCP User responsible for the request
     * @param v the SumEHR import result
     * @param dest the patient to return
     * @return the Patient passed as parameter (if not null) or the one retrieved (if any).
     */
    private suspend fun getExistingPatientWithHcpHierarchy(
        p: PersonType,
        author: User,
        v: ImportResult,
        dest: Patient? = null,
    ): Patient? {
        if (author.healthcarePartyId == null) {
            return null
        }

        val hcp = healthcarePartyLogic.getHealthcareParty(author.healthcarePartyId!!)

        return hcp?.parentId?.let { parentId ->
            userLogic.listUserIdsByHcpartyId(parentId).firstOrNull()?.let { parentAuthorId ->
                userLogic.getUser(parentAuthorId)?.let {parentAuthor ->
                    getExistingPatient(p, parentAuthor, v, dest)
                }
            }
        } ?: getExistingPatient(p, author, v, dest)
            }

    /**
     * If the Patient passed as parameter is not null, returns that. Otherwise, uses the data from the SumEHR to
     * retrieve the patient by Ssin, date of birth or fuzzy name.
     * @param p the Patient data from the SumEHR
     * @param author the HCP User responsible for the request
     * @param v the SumEHR import result
     * @param dest the patient to return
     * @return the Patient passed as parameter (if not null) or the one retrieved (if any).
     */
    private suspend fun getExistingPatient(
        p: PersonType,
        author: User,
        v: ImportResult,
        dest: Patient? = null,
    ): Patient? {
        if (author.healthcarePartyId == null) {
            return null
        }

        val niss = validSsinOrNull(p.ids.find { it.s == IDPATIENTschemes.ID_PATIENT }?.value) // searching empty niss return all patients
        v.notNull(niss, "Niss shouldn't be null for patient $p")

        return dest ?: niss?.let {
            patientLogic.listByHcPartyAndSsinIdsOnly(niss, author.healthcarePartyId!!).firstOrNull()
                ?.let { patientLogic.getPatient(it) }
            } ?: patientLogic.listByHcPartyDateOfBirthIdsOnly(
             Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date) ?: throw IllegalStateException("Person's date of birth is invalid"),
              author.healthcarePartyId!!).toList().takeIf { it.isNotEmpty() }?.let {
                patientLogic.getPatients(it).filter {
                    p.firstnames.any { fn -> equals(it.firstName, fn) && equals(it.lastName, p.familyname) }
                }.firstOrNull()
            } ?: patientLogic.listByHcPartyNameContainsFuzzyIdsOnly(
                sanitizeString(p.familyname + p.firstnames.first()),
                author.healthcarePartyId!!).toList().takeIf { it.isNotEmpty() }?.let {
                    patientLogic.getPatients(it).filter { patient ->
                        patient.dateOfBirth?.let { dateOfBirth ->
                            dateOfBirth == Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date)
                        } ?: false
                    }.firstOrNull()
            }
    }

    /**
     * Finds a Patient related to a HCP using the information contained in the ImportResult. If the Patient does not
     * exist, it creates it and save it to the database
     * @param p a PersonType contained in the SumEHR message
     * @param author the HCP User responsible for the import
     * @param v the Import Result
     * @param saveToDatabase whether saving the new Patient to the database or not
     * @param dest if not null, this Patient is used instead of the retrieved one
     * @return the retrieved or newly created HCP
     */
    private suspend fun createOrProcessPatient(
        p: PersonType,
        author: User,
        v: ImportResult,
        saveToDatabase: Boolean,
        dest: Patient? = null,
    ): Patient? = getExistingPatientWithHcpHierarchy(p, author, v, dest)
        ?: Patient(
            id = idGenerator.newGUID().toString(),
            delegations = author.healthcarePartyId?.let { mapOf(it to setOf()) } ?: mapOf(),
        ).let {
            copyFromPersonToPatient(p, it)
        }.let { if (saveToDatabase) patientLogic.createPatient(it) else it }

    /**
     * Creates a new patient merging the information from an existing patient and the ones retrieved from the SumEHR.
     * @param p the Patient information retrieved from the SumEHR
     * @param patient the existing Patient
     * @param force true to prefer the data retrieved from the SumEHR
     * @return a Patient
     */
    private fun copyFromPersonToPatient(p: PersonType, patient: Patient, force: Boolean = true): Patient =
        patient.copy(
            firstName = p.firstnames.firstOrNull(),
            lastName = p.familyname,
            dateOfBirth = Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date),
            ssin = patient.ssin ?: p.ids.find { it.s == IDPATIENTschemes.ID_PATIENT }?.value
                ?: p.ids.find { it.s == IDPATIENTschemes.INSS }?.value,
            placeOfBirth = if (force || patient.placeOfBirth == null) p.birthlocation?.getFullAddress() else patient.placeOfBirth,
            dateOfDeath = if (force || patient.dateOfDeath == null) p.deathdate?.let { Utils.makeFuzzyIntFromXMLGregorianCalendar(it.date) } else patient.dateOfDeath,
            placeOfDeath = if (force || patient.placeOfDeath == null) p.deathlocation?.getFullAddress() else patient.placeOfDeath,
            gender = if (force || patient.gender == null) {
                when (p.sex.cd.value) {
                    CDSEXvalues.FEMALE -> Gender.female
                    CDSEXvalues.MALE -> Gender.male
                    CDSEXvalues.UNKNOWN -> Gender.unknown
                    CDSEXvalues.CHANGED -> Gender.changed
                    else -> Gender.unknown
                }
            } else {
                patient.gender
            },
            profession = if (force || patient.profession == null) p.profession?.text?.value else patient.profession,
            externalId = p.ids.firstOrNull { i -> i.s == IDPATIENTschemes.LOCAL && i.sl == "PatientReference" }?.value?.let { patRef ->
                if (force || patient.externalId == null) patRef else patient.externalId
            } ?: patient.externalId,
            alias = p.ids.firstOrNull { i -> i.s == IDPATIENTschemes.LOCAL && i.sl == "PatientAlias" }?.value?.let { alias ->
                if (force || patient.externalId == null) alias else patient.alias
            } ?: patient.alias,
            addresses = patient.addresses + (
                p.addresses?.let { addresses ->
                    addresses.map { a ->
                        val addressType = a.cds.find { it.s == CDADDRESSschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) }
                        Address(
                            addressType = addressType,
                            street = a.street,
                            city = a.city,
                            houseNumber = a.housenumber,
                            postboxNumber = a.postboxnumber,
                            postalCode = a.zip,
                            country = a.country?.cd?.value,
                            telecoms = p.telecoms
                                .filter { t ->
                                    t.cds.find { it.s == CDTELECOMschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) } == addressType
                                }.mapNotNull { t ->
                                    t.cds.find { it.s == CDTELECOMschemes.CD_TELECOM }?.let { TelecomType.valueOf(it.value) }?.let { telecomType ->
                                        Telecom(telecomType = telecomType, telecomNumber = t.telecomnumber)
                                    }
                                }
                        )
                    }
                } ?: listOf()
                ),
            languages = patient.languages + (
                p.usuallanguage?.let { if (patient.languages.contains(it)) null else listOf(it) }
                    ?: listOf()
                )
        )
}

private fun selector(
    headingsAndItemsAndTexts: MutableList<Serializable>,
    predicate: ((ItemType) -> Boolean)?,
): List<ItemType> {
    return headingsAndItemsAndTexts.fold(listOf()) { acc, it ->
        when (it) {
            is ItemType -> if (predicate == null || predicate(it)) acc + listOf(it) else acc
            is TextType -> acc
            is HeadingType -> acc + selector(it.headingsAndItemsAndTexts, predicate)
            else -> acc
        }
    }
}

private fun TransactionType.findItem(predicate: ((ItemType) -> Boolean)? = null): ItemType? {
    return selector(this.headingsAndItemsAndTexts, predicate).firstOrNull()
}

private fun TransactionType.findItemsByHeadingId(predicate: ((ItemType) -> Boolean)? = null, headingId: String): List<ItemType> {
    val hits = this.headingsAndItemsAndTexts.filter { it -> it is HeadingType && it.ids.filter { id -> id.value == headingId }.count() > 0 }

    return selector(hits.toMutableList(), predicate)
}

private fun TransactionType.findItems(predicate: ((ItemType) -> Boolean)? = null): List<ItemType> {
    return selector(this.headingsAndItemsAndTexts, predicate)
}

private fun AddressTypeBase.getFullAddress(): String {
    val street = "${street ?: ""}${housenumber?.let { " $it" } ?: ""}${postboxnumber?.let { " b $it" } ?: ""}"
    val city = "${zip ?: ""}${city?.let { " $it" } ?: ""}"
    return listOf(street, city, country?.let { it.cd?.value } ?: "").filter { it.isNotBlank() }.joinToString(";")
}
