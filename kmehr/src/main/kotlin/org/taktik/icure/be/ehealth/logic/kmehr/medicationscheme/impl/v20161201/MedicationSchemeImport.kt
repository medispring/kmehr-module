/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.medicationscheme.impl.v20161201

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils.makeFuzzyLongFromMomentType
import org.taktik.icure.be.ehealth.logic.kmehr.validNihiiOrNull
import org.taktik.icure.be.ehealth.logic.kmehr.validSsinOrNull
import org.taktik.icure.db.equals
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.mapping.ImportMapping
import org.taktik.icure.domain.result.ImportResult
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.Form
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
import org.taktik.icure.entities.embed.Substanceproduct
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.entities.embed.TelecomType
import org.taktik.icure.exceptions.MissingRequirementsException
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDADDRESSschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTYschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes
import org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDLNKvalues
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
import java.io.InputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.time.temporal.ChronoUnit
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement

@Profile("kmehr")
@org.springframework.stereotype.Service
class MedicationSchemeImport(
    val patientLogic: PatientLogic,
    val healthcarePartyLogic: HealthcarePartyLogic,
    private val healthElementLogic: HealthElementLogic,
    val contactLogic: ContactLogic,
    val documentLogic: DocumentLogic,
    private val idGenerator: UUIDGenerator,
) {
    private val defaultMapping: Map<String, List<ImportMapping>> = ObjectMapper().let { om ->
        val txt = this.javaClass.classLoader.getResourceAsStream("org/taktik/icure/be/ehealth/logic/kmehr/smf/impl/smf.labels.json")
            ?.readBytes()?.toString(Charsets.UTF_8) ?: "{}"
        om.readValue(txt)
    }

    fun convertToKmehr(inputStream: InputStream): Kmehrmessage {
        val jc = JAXBContext.newInstance(Kmehrmessage::class.java)
        val unmarshaller = jc.createUnmarshaller()
        return unmarshaller.unmarshal(inputStream) as Kmehrmessage
    }

    /**
     * Converts a Medication Scheme XML message to a set of Contacts, SubContacts, Services, HealthElements,
     * Documents, and Forms of the iCure Data Model.
     * @param inputData the ByteArray containing the XML message.
     * @param author the User responsible for the import.
     * @param language the language of the Content inside the iCure entities.
     * @param saveToDatabase whether to save in the database the new entities created.
     * @param mappings
     * @param dest the Patient object of the SMF.
     * @return a List of ImportResult.
     */
    suspend fun importMedicationSchemeFile(
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

        val myMappings = defaultMapping + mappings

        val state = InternalState()

        val allRes = kmehrMessage.folders.map { folder ->
            ImportResult().also { res ->
                res.patient = createOrProcessPatient(folder.patient, author, res, saveToDatabase, dest)
                res.ctcs.addAll(
                    folder.transactions.map { trn ->
                        when (trn.cds.find { it.s == CDTRANSACTIONschemes.CD_TRANSACTION }?.value) {
                            "medicationscheme" -> parseMedicationScheme(trn, author, res, language, myMappings, saveToDatabase, state)
                            "medicationschemeelement" -> parseMedicationSchemeElement(trn, author, res, language, myMappings, saveToDatabase, state)
                            "treatmentsuspension" -> parseTreatmentSuspension(trn, author, res, language, myMappings, saveToDatabase, state)
                            else -> parseGenericTransaction(trn, author, res, language, myMappings, saveToDatabase, state)
                        }.also { con ->
                            if (saveToDatabase) {
                                contactLogic.createContact(con)
                            }
                            getTransactionMFID(trn)?.let {
                                state.contactsByMFID[it] = con
                            }
                        }
                    },
                )

				/* TODO convert links ISASERVICEFOR to subcontacts
				state.subcontactLinks.groupBy{ it["contact"] as Contact }.forEach{
					val contact = it.key
					it.value.groupBy{ it["heMFID"] as String }.forEach { subentry ->
						val heid = state.hesByMFID[subentry.key]?.id
						heid?.let {
							contact.subContacts.add(
									SubContact().apply {
										healthElementId = heid
										services = subentry.value.map {
											ServiceLink( (it["service"] as Service).id )
										}
									}
							)
						}
					}
				}*/

                // make sure all He versions have the same healthElementId
                state.versionLinksByMFID = state.versionLinks.groupBy { it.mfId } // speed up lookup
                state.versionLinks.forEach { hev ->
                    hev.versionId = findHeAncestor(hev, state)
                }
                res.hes = res.hes.map { he -> he.copy(healthElementId = state.versionLinks.find { it.he.id == he.id }?.versionId ?: he.healthElementId) }.toMutableList()
            }
        }
        return allRes
    }

    /**
     * Finds the oldest version of a HealthElement.
     * @param parentHe a HeVersionType to start from.
     * @param state an InternalState instance.
     * @param walkedMap the HealthElements visited up to now.
     * @return the id of the HealthElement.
     */
    private tailrec fun findHeAncestor(parentHe: HeVersionType, state: InternalState, walkedMap: Map<String, String?> = emptyMap()): String? =
        if (parentHe.isANewVersionOfId == null) {
            // last ancestor
            parentHe.he.healthElementId
        } else {
            val nextAncestor = state.versionLinksByMFID[parentHe.isANewVersionOfId]?.find {
                it.he.id != parentHe.he.id && walkedMap[it.he.id] == null && it.mfId == parentHe.isANewVersionOfId
            }
            if (nextAncestor != null) // found ancestor, look for his ancestor
                findHeAncestor(nextAncestor, state, walkedMap + (parentHe.he.id to "done"))
            else {
                // there is a link but no ancestor found, ignore the link
                println("WARNING: MFID ${parentHe.mfId} links to ${parentHe.isANewVersionOfId} but the target cannot be found")
                parentHe.he.healthElementId
            }
        }

    /**
     * Converts a TransactionType to an iCure Contact. It parses all the ItemTypes the transaction has as Services,
     * SubContacts, and HealthElements.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ImportResult.
     * @param language the language of the Content of Services and HealthElements.
     * @param mappings
     * @param saveToDatabase whether to save the new entities to the database.
     * @param state the InternalState
     * @return an iCure Contact.
     */
    private suspend fun parseMedicationScheme(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        state: InternalState,
    ): Contact = parseGenericTransaction(trn, author, v, language, mappings, saveToDatabase, state)

    /**
     * Converts a TransactionType to an iCure Contact. It parses all the ItemTypes the transaction has as Services,
     * SubContacts, and HealthElements.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ImportResult.
     * @param language the language of the Content of Services and HealthElements.
     * @param mappings
     * @param saveToDatabase whether to save the new entities to the database.
     * @param state the InternalState
     * @return an iCure Contact.
     */
    private suspend fun parseMedicationSchemeElement(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        state: InternalState,
    ): Contact = parseGenericTransaction(trn, author, v, language, mappings, saveToDatabase, state)

    /**
     * Converts a TransactionType to an iCure Contact. It parses all the ItemTypes the transaction has as Services,
     * SubContacts, and HealthElements.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ImportResult.
     * @param language the language of the Content of Services and HealthElements.
     * @param mappings
     * @param saveToDatabase whether to save the new entities to the database.
     * @param state the InternalState
     * @return an iCure Contact.
     */
    private suspend fun parseTreatmentSuspension(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        state: InternalState,
    ): Contact = parseGenericTransaction(trn, author, v, language, mappings, saveToDatabase, state)

    /**
     * Converts a TransactionType to an iCure Contact. It parses all the ItemTypes the transaction has as Services,
     * SubContacts, and HealthElements.
     * @param trn the TransactionType.
     * @param author the User responsible for the import.
     * @param v the ImportResult.
     * @param language the language of the Content of Services and HealthElements.
     * @param mappings
     * @param saveToDatabase whether to save the new entities to the database.
     * @param state the InternalState
     * @return an iCure Contact.
     */
    private suspend fun parseGenericTransaction(
        trn: TransactionType,
        author: User,
        v: ImportResult,
        language: String,
        mappings: Map<String, List<ImportMapping>>,
        saveToDatabase: Boolean,
        state: InternalState,
    ): Contact {
        val contactDate = trn.date?.let { Utils.makeFuzzyLongFromDateAndTime(it, trn.time) }
            ?: trn.findItem { it.cds.any { cd -> cd.s == CDITEMschemes.CD_ITEM && cd.value == "encounterdatetime" } }
                ?.let { cd -> cd.contents?.find { it.date != null }?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) } }

        val contactId = idGenerator.newGUID().toString()
        return Contact(
            id = contactId,
            author = author.id,
            responsible = trn.author?.hcparties
                ?.filter { hcpId -> hcpId.cds.any { it.s == CDHCPARTYschemes.CD_HCPARTY && it.value == "persphysician" } }
                ?.firstNotNullOfOrNull {
                    createOrProcessHcp(it, saveToDatabase)?.also { hcp ->
                        v.hcps.add(hcp)
                    }
                }?.id ?: author.healthcarePartyId,
            openingDate = contactDate,
            closingDate = trn.isIscomplete.let { if (it) contactDate else null },
            location =
                trn.findItem { it: ItemType -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encounterlocation" } }
                    ?.let { itemType ->
                        itemType.contents?.flatMap { it.texts.map { t -> t.value } }?.joinToString(",")
                    },
            encounterType = trn.findItem { it: ItemType -> it.cds.any { it.s == CDITEMschemes.CD_ITEM && it.value == "encountertype" } }
                ?.let {
                    it.contents?.firstNotNullOfOrNull { contentType ->
                        contentType.cds
                            ?.find { cd -> cd.s == CDCONTENTschemes.CD_ENCOUNTER }
                            ?.let { cdContent ->
                                CodeStub.from("CD-ENCOUNTER", cdContent.value, "1.0")
                            }
                    }
                } ?: CodeStub.from("CD-ENCOUNTER", "consultation", "1.0"),
            services = trn.findItems().mapNotNull { item ->
                val cdItem = item.cds.find { it.s == CDITEMschemes.CD_ITEM }?.value ?: "note"
                val mapping =
                    mappings[cdItem]?.find { (it.lifecycle == "*" || it.lifecycle == item.lifecycle?.cd?.value?.value()) && ((it.content == "*") || item.hasContentOfType(it.content)) }
                val label =
                    (item.cds.find { it.s == CDITEMschemes.LOCAL && it.sl == "org.taktik.icure.label" }?.value
                        ?: mapping?.label?.get(language)
                        ?: item.contents.filter { (it.texts?.size ?: 0) > 0 }
                            .flatMap { contentType ->
                                contentType.texts.filter {
                                    it.l == language
                                }.map {
                                    it.value
                                }
                            }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(" ")
                        ?: mappings["note"]?.lastOrNull()?.label?.get(language)
                        ?: "Note").takeIf { cdItem != "parameter" } ?: "Observation"
                when (cdItem) {
                    "healthcareelement" -> {
                        val he = parseHealthcareElement(mapping?.tags?.find { it.type == "CD-ITEM" }?.code ?: cdItem, label, item, author, contactId)
                        he.let { notNullHe ->
                            v.hes.add(if (saveToDatabase) healthElementLogic.createEntities(listOf(he)).firstOrNull() ?: throw (IllegalStateException("Cannot save to database")) else he)
                            // register new version links
                            getItemMFID(item)?.let { mfId ->
                                state.versionLinks.add(
                                    HeVersionType(
                                        he = notNullHe,
                                        mfId = mfId,
                                        isANewVersionOfId = item.lnks.find { it.type == CDLNKvalues.ISANEWVERSIONOF }?.let {
                                            extractMFIDFromUrl(it.url)
                                        },
                                        versionId = null,
                                    ),
                                )
                                state.hesByMFID[mfId] = notNullHe
                            }
                        }
                        null
                    }
                    "encountertype", "encounterdatetime", "encounterlocation" -> null // already added at contact level
                    "insurancystatus", "gmdmanager" -> null // not services
                    else -> {
                        parseGenericItem(mapping?.tags?.find { it.type == "CD-ITEM" }?.code ?: cdItem, label, item, author, language).also { service ->
                            item.lnks.filter { it.type == CDLNKvalues.ISASERVICEFOR }.mapNotNull {
                                extractMFIDFromUrl(it.url)
                            }.map {
                                state.subcontactLinks.add(
                                    mapOf(
                                        "service" to service,
                                        "heMFID" to it,
                                        "contact" to this,
                                    ),
                                )
                            }
                        }
                    }
                }
            }.toSet(),
        )
    }

    /**
     * Creates a new HealthElement using the information contained in an ItemType.
     * @param cdItem the code of the tag to be added to the HealthElement.
     * @param label the label to use if the ItemType contains no description.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param contactId the id of the opening contact of the HealthElement.
     * @return a HealthElement.
     */
    private fun parseHealthcareElement(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        contactId: String,
    ): HealthElement {
        val healthElementDate = item.beginmoment
            ?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) } ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)
        val healthElementCreated = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli()
        return HealthElement(
            id = idGenerator.newGUID().toString(),
            healthElementId = idGenerator.newGUID().toString(),
            descr = if (item.texts.isNotEmpty()) {
                "$label, ${item.texts.joinToString(" ") { it.value }}"
            } else {
                label
            },
            tags = setOf(CodeStub.from("CD-ITEM", cdItem, "1")) +
                extractTags(item) +
                (item.lifecycle?.let { setOf(CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1")) } ?: setOf()),
            author = author.id,
            responsible = author.healthcarePartyId,
            codes = extractCodes(item).toMutableSet(),
            valueDate = healthElementDate,
            openingDate = healthElementDate,
            closingDate = item.endmoment?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) },
            idOpeningContact = contactId,
            created = healthElementCreated,
            modified = healthElementCreated,
            status = ((item.lifecycle?.cd?.value?.value()
                ?.let {
                    when (it) {
                        "inactive", "aborted", "canceled" -> 1
                        "notpresent", "excluded" -> 4
                        else -> 0
                    }
                } ?: 0) + if (item.isIsrelevant != true) 2 else 0)
        )
    }

    /**
     * Extract all the codes from an ItemTypes and returns them as a Set of CodeStub.
     * @param item The ItemType.
     * @return A Set of CodeStub.
     */
    private fun extractCodes(item: ItemType): Set<CodeStub> =
        (item.cds
            .filter { it.s == CDITEMschemes.ICPC || it.s == CDITEMschemes.ICD }
            .map { CodeStub.from(it.s.value(), it.value, it.sv) } +
        item.contents
            .filter { (it.cds?.size ?: 0) > 0 }
            .flatMap { contentType ->
                contentType.cds.filter {
                    listOf(
                        CDCONTENTschemes.CD_DRUG_CNK,
                        CDCONTENTschemes.ICD,
                        CDCONTENTschemes.ICPC,
                        CDCONTENTschemes.CD_CLINICAL,
                        CDCONTENTschemes.CD_ATC,
                        CDCONTENTschemes.CD_PATIENTWILL,
                        CDCONTENTschemes.CD_VACCINEINDICATION,
                    ).contains(it.s)
                }.map { CodeStub.from(it.s.value(), it.value, it.sv) }
        }).toSet()

    /**
     * Extracts all the tags from the CD_PARAMETER, CD_LAB, CD_TECHNICAL, CD_CONTACT_PERSON, and LOCAL CD-ITEM schemes
     * of an ItemType and returns them as a Collection of CodeStubs.
     * @param item The ItemType.
     * @return A Collection of CodeStubs.
     */
    private fun extractTags(item: ItemType): Collection<CodeStub> =
        (item.cds
            .filter { it.s == CDITEMschemes.CD_PARAMETER || it.s == CDITEMschemes.CD_LAB || it.s == CDITEMschemes.CD_TECHNICAL }
            .map { CodeStub.from(it.s.value(), it.value, it.sv) } +
        item.contents.filter { (it.cds?.size ?: 0) > 0 }.flatMap { contentType ->
            contentType.cds.filter {
                listOf(CDCONTENTschemes.CD_LAB).contains(it.s)
            }.map { CodeStub.from(it.s.value(), it.value, it.sv) }
        }).toSet()

    /**
     * Converts a KmEHR ItemType to a Service. It adds as content the data
     * about medication and prescriptions.
     * @param cdItem the codes to add.
     * @param label the label of the Service.
     * @param item the ItemType.
     * @param author the User responsible for the import.
     * @param language the language of the Service content.
     * @return an iCure Service.
     */
    private fun parseGenericItem(
        cdItem: String,
        label: String,
        item: ItemType,
        author: User,
        language: String,
    ): Service {
        val serviceDate = item.beginmoment
            ?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) }
            ?: item.recorddatetime?.let { Utils.makeFuzzyLongFromXMLGregorianCalendar(it) } ?: FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.SECONDS)
        val serviceCreatedDate = item.recorddatetime?.toGregorianCalendar()?.toInstant()?.toEpochMilli()
        val tags = listOf(CodeStub.from("CD-ITEM", cdItem, "1"))+
                extractTags(item) +
                (item.temporality?.cd?.value?.let { listOf(CodeStub.from("CD-TEMPORALITY", it.value(), "1")) } ?: listOf()) +
                (item.lifecycle?.let { listOf(CodeStub.from("CD-LIFECYCLE", it.cd.value.value(), "1")) } ?: listOf())
        return Service(
            id = idGenerator.newGUID().toString(),
            label = tags.find { it.type == "CD-PARAMETER" }?.let {
                consultationFormMeasureLabels[it.code]
            } ?: if (item.contents.any { it.substanceproduct != null || it.medicinalproduct != null || it.compoundprescription != null }) "Medication" else label,
            codes = extractCodes(item).toSet(),
            responsible = author.healthcarePartyId,
            valueDate = serviceDate,
            openingDate = serviceDate,
            closingDate = item.endmoment?.let { Utils.makeFuzzyLongFromDateAndTime(it.date, it.time) },
            created = serviceCreatedDate,
            modified = serviceCreatedDate,
            status = ((item.lifecycle?.cd?.value?.value()
                ?.let {
                    when (it) {
                        "inactive", "aborted", "canceled" -> 1
                        "notpresent", "excluded" -> 4
                        else -> 0
                    }
                } ?: 0) + if (item.isIsrelevant != true) 2 else 0),
            content = when {
                (item.contents.any { it.substanceproduct != null || it.medicinalproduct != null || it.compoundprescription != null }) -> {
                    Content(
                        medicationValue = Medication(
                            substanceProduct = item.contents.firstOrNull { it.substanceproduct != null }?.let { contentType ->
                                contentType.substanceproduct?.let {
                                    Substanceproduct(
                                        intendedcds = it.intendedcd?.let { cd -> listOf(CodeStub.from(cd.s.value(), cd.value, cd.sv)) } ?: listOf(),
                                        intendedname = it.intendedname.toString(),
                                    )
                                }
                            },
                            medicinalProduct = item.contents.firstOrNull { it.medicinalproduct != null }?.let { contentType ->
                                contentType.medicinalproduct?.let { product ->
                                    Medicinalproduct(
                                        intendedcds = product.intendedcds?.map { CodeStub.from(it.s.value(), it.value, it.sv) } ?: listOf(),
                                        intendedname = product.intendedname.toString(),
                                    )
                                }
                            },
                            compoundPrescription = item.contents.firstOrNull {
                                it.compoundprescription?.content?.isNotEmpty() ?: false
                            }?.let {
                                // spec is unclear, some software put text in <magistraltext> some put it directly in compoundprescription
                                // try to detect each case
                                it.compoundprescription?.content?.mapNotNull { content ->
                                    // spec is unclear, some software put text in <magistraltext> some put it directly in compoundprescription
                                    // try to detect each case
                                    if (content is String) {
                                        content
                                    } else {
                                        if (content is org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20131001.be.fgov.ehealth.standards.kmehr.dt.v1.TextType) {
                                            content.value
                                        } else {
                                            try {
                                                if ((content as JAXBElement<*>).value is org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20131001.be.fgov.ehealth.standards.kmehr.dt.v1.TextType) {
                                                    (content.value as org.taktik.icure.services.external.rest.v1.dto.be.ehealth.kmehr.v20131001.be.fgov.ehealth.standards.kmehr.dt.v1.TextType).value
                                                } else {
                                                    null
                                                }
                                            } catch (ex: Exception) {
                                                null
                                            }
                                        }
                                    }
                                }?.joinToString(" ") { itt -> itt.trim() }
                            } ?: "",
                            instructionForPatient = item.instructionforpatient?.value +
                                    (item.lnks
                                        .mapNotNull { it.value?.toString(Charsets.UTF_8) }
                                        .joinToString(", ")
                                        .takeIf { it.isNotBlank() }),
                            posology = item.posology?.text?.value, // posology can be complex but SMF spec recommends text type
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
                            beginMoment = item.beginmoment?.let { makeFuzzyLongFromMomentType(it) },
                            endMoment = item.endmoment?.let { makeFuzzyLongFromMomentType(it) },
                        )
                    )
                }
                (item.contents.any { it.decimal != null }) -> item.contents.firstOrNull { it.decimal != null }?.let {
                    if (it.unit != null) {
                        Content(measureValue = Measure(value = it.decimal.toDouble(), unit = it.unit?.cd?.value))
                    } else {
                        Content(numberValue = it.decimal.toDouble())
                    }
                }
                (item.contents.any { it.texts.any { t -> t.value?.isNotBlank() ?: false } }) -> {
                    val textValue = item.contents
                        .filter { (it.texts?.size ?: 0) > 0 }
                        .flatMap { it.texts.map { t -> t.value } }
                        .joinToString(", ")
                        .takeIf { it.isNotBlank() }
                    val measureValue = if (cdItem == "parameter") {
                        // Try harder to convert to measure
                        item.contents.filter { (it.texts?.size ?: 0) > 0 }.flatMap { content ->
                            content.texts.map { textType ->
                                textType.value?.let {
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
                        }.filterNotNull().firstOrNull()
                    } else {
                        null
                    }
                    if (measureValue == null) {
                        Content(stringValue = textValue)
                    } else {
                        Content(measureValue = measureValue)
                    }
                }
                (item.contents.any { it.isBoolean != null }) -> item.contents.firstOrNull { it.isBoolean != null }?.let {
                    Content(booleanValue = it.isBoolean)
                }
                else -> null
            }?.let { mapOf(language to it) } ?: mapOf(),

        )
    }

    /**
     * Checks if an ItemType has a Content of the type passed as parameter.
     * @param content the Content
     * @return true if the ItemType has the content, false otherwise
     */
    private fun ItemType.hasContentOfType(content: String?): Boolean =
        content?.let { c ->
            c == "m" && this.contents.any { it.medicinalproduct != null || it.substanceproduct != null || it.compoundprescription != null } ||
                    c == "s" && this.contents.any { (it.texts?.size ?: 0) > 0 || (it.cds?.size ?: 0) > 0 || it.hcparty != null }
        } ?: true

    /**
     * Given a HcpartyType, tries to retrieve the corresponding iCure HealthcareParty by Nihii, by Niss and ultimately
     * by name and adds it to the ImportResult. If the HCP does not exist, it creates it and saves it to the db.
     * @param p The HcpartyType
     * @return a HCP
     */
    private suspend fun createOrProcessHcp(p: HcpartyType, saveToDatabase: Boolean): HealthcareParty? {
        val nihii = validNihiiOrNull(p.ids.find { it.s == IDHCPARTYschemes.ID_HCPARTY }?.value)
        val niss = validSsinOrNull(p.ids.find { it.s == IDHCPARTYschemes.INSS }?.value)

        return nihii?.let { healthcarePartyLogic.listHealthcarePartiesByNihii(it).firstOrNull() }
            ?: niss?.let { healthcarePartyLogic.listHealthcarePartiesBySsin(niss).firstOrNull() }
            ?: try {
                copyFromHcpToHcp(p, HealthcareParty(id = idGenerator.newGUID().toString(), nihii = nihii, ssin = niss)).also {
                    if (saveToDatabase) healthcarePartyLogic.createHealthcareParty(it)
                }
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
                    addresses.map { address ->
                        val addressType = address.cds.find { it.s == CDADDRESSschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) }
                        Address(
                            addressType = addressType,
                            street = address.street,
                            city = address.city,
                            houseNumber = address.housenumber,
                            postboxNumber = address.postboxnumber,
                            postalCode = address.zip,
                            country = address.country?.cd?.value,
                            telecoms = p.telecoms.filter { t ->
                                t.cds.find { it.s == CDTELECOMschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) } == addressType }
                                .mapNotNull { tt ->
                                    tt.cds.find { it.s == CDTELECOMschemes.CD_TELECOM }?.let { TelecomType.valueOf(it.value) }?.let { telecomType ->
                                        Telecom(telecomType = telecomType, telecomNumber = tt.telecomnumber)
                                    }
                                }
                        )
                    }
                } ?: listOf()
            )
        )

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

        val dbPatient =
            dest ?: niss?.let {
                patientLogic.listByHcPartyAndSsinIdsOnly(niss, author.healthcarePartyId!!).firstOrNull()
                    ?.let { patientLogic.getPatient(it) }
            } ?: patientLogic.listByHcPartyDateOfBirthIdsOnly(
                Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date) ?: throw IllegalStateException("Person's date of birth is invalid"),
                author.healthcarePartyId!!
            ).toList().let {
                if (it.isNotEmpty()) {
                    patientLogic.getPatients(it).filter {
                        p.firstnames.any { fn -> equals(it.firstName, fn) && equals(it.lastName, p.familyname) }
                    }.firstOrNull()
                } else {
                    null
                }
            } ?: patientLogic.listByHcPartyNameContainsFuzzyIdsOnly(
                sanitizeString(p.familyname + p.firstnames.first()),
                author.healthcarePartyId!!
            ).toList().let {
                if (it.isNotEmpty()) {
                    patientLogic.getPatients(it).filter { patient ->
                        patient.dateOfBirth?.let { dob -> dob == Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date) }
                            ?: false
                    }.firstOrNull()
                } else null
            }
        return dbPatient
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
    ): Patient? {
        if (author.healthcarePartyId == null) {
            return null
        }
        return getExistingPatient(p, author, v, dest)
            ?: copyFromPersonToPatient(p, Patient(id = idGenerator.newGUID().toString(), delegations = mapOf(author.healthcarePartyId!! to setOf())))
                .let { if (saveToDatabase) patientLogic.createPatient(it) else it }
    }

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
            dateOfBirth = org.taktik.icure.be.ehealth.dto.kmehr.v20131001.Utils.makeFuzzyIntFromXMLGregorianCalendar(p.birthdate.date),
            ssin = patient.ssin ?: p.ids.find { it.s == IDPATIENTschemes.ID_PATIENT }?.value ?: p.ids.find { it.s == IDPATIENTschemes.INSS }?.value,
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
                    addresses.map { address ->
                        val addressType = address.cds.find { it.s == CDADDRESSschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) }
                        Address(
                            addressType = addressType,
                            street = address.street,
                            city = address.city,
                            houseNumber = address.housenumber,
                            postboxNumber = address.postboxnumber,
                            postalCode = address.zip,
                            country = address.country?.cd?.value,
                            telecoms = p.telecoms.filter { t ->
                                t.cds.find { it.s == CDTELECOMschemes.CD_ADDRESS }?.let { AddressType.valueOf(it.value) } == addressType
                            }.mapNotNull { tt ->
                                tt.cds.find { it.s == CDTELECOMschemes.CD_TELECOM }?.let { TelecomType.valueOf(it.value) }?.let { telecomType ->
                                    Telecom(telecomType = telecomType, telecomNumber = tt.telecomnumber)
                                }
                            }
                        )
                    }
                } ?: listOf()),
            languages = patient.languages + (p.usuallanguage?.let { if (patient.languages.contains(it)) null else listOf(it) } ?: listOf()),
        )

    /**
     * Extract the MF-ID from an url using Regex.
     * @param url The url.
     * @return the MF-ID, if any.
     */
    private fun extractMFIDFromUrl(url: String): String? {
        val regex = Regex("SL=\"MF-ID\"\\sand\\s\\.=\"([^\"]+)\"")
        val result = regex.find(url)
        return result?.groups?.get(1)?.value?.trim()
    }

    /**
     * Finds the first MF-ID with local scheme in the ItemType passed as parameter.
     * @param item The ItemType.
     * @return the MF-ID, if any.
     */
    private fun getItemMFID(item: ItemType): String? =
        item.ids.find { it.s == IDKMEHRschemes.LOCAL && it.sl == "MF-ID" }?.value

    /**
     * Finds the first MF-ID with local scheme in the TransactionType passed as parameter.
     * @param trn The TransactionType.
     * @return the MF-ID, if any.
     */
    private fun getTransactionMFID(trn: TransactionType): String? =
        trn.ids.find { it.s == IDKMEHRschemes.LOCAL && it.sl == "MF-ID" }?.value

    val consultationFormMeasureLabels: Map<String, String> = mapOf(
        // theses labels are used to identify services associated to form consultation
        // should be lower case
        "weight" to "Poids",
        "height" to "Taille",
        "bmi" to "BMI",
        "heartpulse" to "Pouls",
        // "craneperim" to  "??",
        "hipperim" to "Tour de taille",
        "glycemy" to "Glyc.", // only in form Consultation 09b8db54-84a3-42e7-b8db-5484a352e77f
        "glycemyhba1c" to "HbA1c",
        "pulse" to "R\u00e9gularit\u00e9 du pouls",
        // "apgarscore" to  "??",
        "systolic" to "Tension art\u00e9rielle systolique",
        "diastolic" to "Tension art\u00e9rielle diastolique",
        "temperature" to "T\u00b0",
        // and compound "tension"
    )

    private data class HeVersionType(val he: HealthElement, val mfId: String, val isANewVersionOfId: String?, var versionId: String?)

    // internal bookkeeping
    private data class InternalState(
        var subcontactLinks: MutableList<Map<String, Any>> = mutableListOf(), // bookkeeping for linking He to Services (map of heId and linked Service/He)
        var versionLinks: MutableList<HeVersionType> = mutableListOf(), // bookkeeping for versioning HealthElements
        var versionLinksByMFID: Map<String, List<HeVersionType>> = mapOf(),
        var hesByMFID: MutableMap<String, HealthElement> = mutableMapOf(),
        var contactsByMFID: MutableMap<String, Contact> = mutableMapOf(),
        var docLinks: MutableList<Pair<Service, String?>> = mutableListOf(), // services, linked parent contactMFId
        var prescLinks: MutableList<Pair<List<Service>, String?>> = mutableListOf(), // services, linked parent contactMFId
        var formServices: MutableMap<String, Service> = mutableMapOf(), // services to not add to dynamic form because already in a form
        var incapacityForms: MutableList<Form> = mutableListOf(), // to add them to parent consultation form
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

private fun TransactionType.findItems(predicate: ((ItemType) -> Boolean)? = null): List<ItemType> {
    return selector(this.headingsAndItemsAndTexts, predicate)
}

private fun AddressTypeBase.getFullAddress(): String {
    val street = "${street ?: ""}${housenumber?.let { " $it" } ?: ""}${postboxnumber?.let { " b $it" } ?: ""}"
    val city = "${zip ?: ""}${city?.let { " $it" } ?: ""}"
    return listOf(street, city, country?.let { it.cd?.value } ?: "").filter { it.isNotBlank() }.joinToString(";")
}
