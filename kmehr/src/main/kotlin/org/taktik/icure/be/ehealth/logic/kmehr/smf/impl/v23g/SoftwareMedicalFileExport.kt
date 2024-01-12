/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.smf.impl.v23g

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asynclogic.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.Utils.makeMomentType
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.Utils.makeXMLGregorianCalendarFromFuzzyLong
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.Utils.makeXmlGregorianCalendar
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.cd.v1.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.id.v1.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20170901.be.fgov.ehealth.standards.kmehr.schema.v1.*
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.emitMessage
import org.taktik.icure.be.ehealth.logic.kmehr.v20170901.KmehrExport
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.entities.*
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.entities.base.ICureDocument
import org.taktik.icure.entities.embed.Insurability
import org.taktik.icure.entities.embed.ReferralPeriod
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.services.external.api.AsyncDecrypt
import org.taktik.icure.services.external.http.websocket.AsyncProgress
import org.taktik.icure.services.external.rest.v1.dto.ContactDto
import org.taktik.icure.services.external.rest.v1.dto.DocumentDto
import org.taktik.icure.services.external.rest.v1.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v1.mapper.ContactMapper
import org.taktik.icure.services.external.rest.v1.mapper.DocumentMapper
import org.taktik.icure.services.external.rest.v1.mapper.HealthElementMapper
import org.taktik.icure.utils.FuzzyValues
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.xml.datatype.DatatypeConstants

@Profile("kmehr")
@org.springframework.stereotype.Service
class SoftwareMedicalFileExport(
    codeLogic: CodeLogic,
    documentLogic: DocumentLogic,
    kmehrConfiguration: KmehrConfiguration,
    private val userLogic: UserLogic,
    private val contactLogic: ContactLogic,
    private val healthcarePartyLogic: HealthcarePartyLogic,
    private val healthElementLogic: HealthElementLogic,
    private val patientLogic: PatientLogic,
    private val insuranceLogic: InsuranceLogic,
    private val contactMapper: ContactMapper,
    private val documentMapper: DocumentMapper,
    private val healthElementMapper: HealthElementMapper
) : KmehrExport(codeLogic,documentLogic, kmehrConfiguration) {

    private var hesByContactId: Map<String?, List<HealthElement>> = mutableMapOf()
    private var servicesByContactId: Map<String?, List<Service>> = mutableMapOf()
    private var newestServicesById: MutableMap<String?, Service> = mutableMapOf()
    private var itemByServiceId: MutableMap<String, ItemType> = mutableMapOf()
    private var oldestHeByHeId: Map<String?, HealthElement> = mutableMapOf()
    private var heById: Map<String?, List<HealthElement>> = mutableMapOf()

    /**
     * Exports the medical data of the Patient in the SMF format, that returns as a Flow of DataBuffer.
     * @param patient the iCure Patient.
     * @param sfks the Patient Secret Foreign Keys.
     * @param sender the HCP responsible for the export.
     * @param language the language of the Content.
     * @param decryptor the Decryptor for the Documents and other entities.
     * @param progressor Used to show the progress of the operation.
     * @param config the Config.
     * @return a Flow of DataBuffer containing the XML message.
     */
    fun exportSMF(
        patient: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        language: String,
        decryptor: AsyncDecrypt?,
        progressor: AsyncProgress?,
        config: Config = Config(
            _kmehrId = System.currentTimeMillis().toString(),
            date = makeXGC(Instant.now().toEpochMilli())!!,
            time = makeXGC(Instant.now().toEpochMilli())!!,
            soft = Config.Software(name = "iCure", version = kmehrConfig.kmehrVersion),
            clinicalSummaryType = "TODO", // not used
            defaultLanguage = "en",
            format = Config.Format.SMF,
            ),
    ): Flow<DataBuffer> = flow {
        log.info("Starting SMF export for ${patient.id}")
        // fill missing config with default values
        config._kmehrId = config._kmehrId ?: System.currentTimeMillis().toString()
        config.date = config.date ?: makeXGC(Instant.now().toEpochMilli())!!
        config.time = config.time ?: makeXGC(Instant.now().toEpochMilli())!!
        config.soft = config.soft ?: Config.Software(name = "iCure", version = kmehrConfig.kmehrVersion)
        config.defaultLanguage = config.defaultLanguage ?: "en"
        config.format = config.format ?: Config.Format.SMF

        itemByServiceId = mutableMapOf()

        val sfksUniq = sfks.toSet().toList() // duplicated sfk cause couchDb views to return duplicated results

        val message = initializeMessage(sender, config)
        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(
                    HcpartyType().apply {
                        cds.add(CDHCPARTY().apply { s = CDHCPARTYschemes.CD_HCPARTY; value = "application" })
                        name = if (config.format == Config.Format.PMF) {
                            "gp-patient-migration"
                        } else {
                            "gp-software-migration"
                        }
                    },
                )
            },
        )

        val folder = makePatientFolder(1, patient, sfksUniq, sender, config, language, decryptor, progressor)
        emitMessage(message.apply { folders.add(folder) }).collect { emit(it) }
    }

    /**
     * Creates a Folder using the medical data of the Patient that the HCP can access.
     * @param patientIndex the KmEHR id of the folder.
     * @param patient the iCure Patient.
     * @param sfks the Patient Secret Foreign Keys.
     * @param healthcareParty the HCP responsible for the export.
     * @param config the Config.
     * @param language the language of the Content.
     * @param decryptor the Decryptor for the Documents and other entities.
     * @param progressor Used to show the progress of the operation.
     * @return a FolderType.
     */
    private suspend fun makePatientFolder(
        patientIndex: Int,
        patient: Patient,
        sfks: List<String>,
        healthcareParty: HealthcareParty,
        config: Config,
        language: String,
        decryptor: AsyncDecrypt?,
        progressor: AsyncProgress?,
    ): FolderType {
        log.info("Make patient export for ${patient.id}")
        val folder = FolderType().apply {
            ids.add(idKmehr(patientIndex))
            this.patient = makePatient(patient, config)
        }
        folder.transactions.add(
            TransactionType().apply {
                ids.add(idKmehr(0))
                ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; sv = "1.0"; value = UUIDGenerator().newGUID().toString() })
                cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "clinicalsummary" })
                date = config.date
                time = config.time
                author = AuthorType().apply {
                    hcparties.add(
                        healthcarePartyLogic.getHealthcareParty(patient.author?.let { userLogic.getUser(it)?.healthcarePartyId } ?: healthcareParty.id)?.let { createParty(it) },
                    )
                }
                isIscomplete = true
                isIsvalidated = true
                getLastGmdManager(patient).let { (hcp, period) ->
                    if (hcp != null && period != null) {
                        makeGmdManager(headingsAndItemsAndTexts.size + 1, config, hcp, period)
                            ?.let { headingsAndItemsAndTexts.add(it) }
                    }
                }
                headingsAndItemsAndTexts.addAll(makeContactPeople(headingsAndItemsAndTexts.size + 1, patient, config))
                makeInsurancyStatus(
                    headingsAndItemsAndTexts.size + 1,
                    config,
                    patient.insurabilities.find {
                        it.endDate == null || FuzzyValues.getDateTime(it.endDate!!)?.isAfter(LocalDateTime.now()) == true
                    })?.let { headingsAndItemsAndTexts.add(it) }
            },
        )

        val contacts = getAllContacts(healthcareParty, sfks.toList()).sortedBy {
            it.openingDate
        }
        val startIndex = folder.transactions.size

        val nonConfidentialHealthElements = getNonConfidentialItems(getHealthElements(healthcareParty, sfks, config))
            .let {
                decryptor?.decrypt(it.map(healthElementMapper::map), HealthElementDto::class.java)?.map(healthElementMapper::map)
                    ?: it
            }

        hesByContactId = nonConfidentialHealthElements.groupBy {
            it.idOpeningContact
        }

        // in PMF, we only want the last version, older versions are removed from servicesByContactId
        servicesByContactId = contacts.associate { con ->
            con.id to con.services.toList().map { svc ->
                svc.also { it.id.let { svcId -> newestServicesById[svcId] = svc } }
            }
        }

        val hesByHeIdSortedByDate = nonConfidentialHealthElements.groupBy {
            it.healthElementId
        }.mapValues { (_, value) ->
            value.sortedWith(compareBy({ it.created }, { it.modified })) // created is the key, but use modified for backward compat
            // oldest He is first in list
        }
        oldestHeByHeId = hesByHeIdSortedByDate.mapValues {
            it.value.first()
        }

        if (config.format == Config.Format.PMF) { // only last version in PMF
            hesByContactId = hesByContactId.map { entry ->
                entry.key to entry.value.filter { he ->
                    hesByHeIdSortedByDate[he.healthElementId]?.last()?.id == he.id
                }
            }.toMap()
        }

        // add Hes without idOpeningContact to clinical summary
        hesByContactId[null].orEmpty().map { he -> addHealthCareElement(
            trn = folder.transactions.first(),
            eds = he,
            config = config,
            language = language) }
        hesByContactId = hesByContactId.filterKeys { it != null }

        heById = nonConfidentialHealthElements.groupBy {
            // retrieve the healthElementId property of an HE by his couchdb id
            it.id
        }

        var documents = emptyList<Triple<String, Service, Contact>>()
        var pharmaceuticalPrescriptions = emptyList<Pair<Service, Contact>>()
        val specialPrescriptions = mutableListOf<TransactionType>()
        val summaries = mutableListOf<TransactionType>()

        contacts.forEachIndexed contactsLoop@{ index, encContact ->
            progressor?.progress((1.0 * index) / (contacts.size + documents.size))
            log.info("Treating contact $index/${contacts.size}")

            var contact: Contact? = null
            if (decryptor != null) {
                log.info("Decrypt ${encContact.id}")
                val ctcDto = contactMapper.map(encContact)
                contact = decryptor.decrypt(listOf(ctcDto), ContactDto::class.java).firstOrNull()?.let { contactMapper.map(it) }
                log.info("${encContact.id} decrypted")
            }

            if (contact == null) return@contactsLoop
            val subContacts = if (config.soft?.name == "Medispring")
                    contact.subContacts.filter { subContact -> subContact.healthElementId == null && subContact.planOfActionId == null && subContact.formId != null }
                else contact.subContacts.toList()
            subContacts.forEachIndexed { idx, subContact ->
                folder.transactions.add(
                    TransactionType().apply {
                        val trn = this

                        val (cdTransactionRef, defaultCdItemRef, exportAsDocument) = when {
                            contact.encounterType?.code == "labresult" -> Triple("labresult", "lab", true)
                            contact.tags.any { it.type == "CD-TRANSACTION" && it.code == "labresult" } -> Triple("labresult", "lab", true)
                            else -> Triple("contactreport", "parameter", false)
                        }
                        val transactionMFID = if (idx > 0) UUIDGenerator().newGUID().toString() else contact.id
                        val parentContactId = contact.id.takeIf { idx > 0 }
                        val services = contact.services.filter { subContact.services.map { s -> s.serviceId }.contains(it.id) }
                        ids.add(idKmehr(startIndex))
                        ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; value = transactionMFID })
                        cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = cdTransactionRef; dn = subContact.descr ?: contact.descr })
                        (contact.modified ?: contact.created)?.let {
                            date = makeXGC(it)
                            time = makeXGC(it)
                        } ?: also {
                            date = config.date
                            time = makeXGC(0L)
                        }
                        (contact.responsible ?: healthcareParty.id).let {
                            author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
                        }
                        isIscomplete = true
                        isIsvalidated = true
                        contact.openingDate?.let { headingsAndItemsAndTexts.add(makeEncounterDateTime(headingsAndItemsAndTexts.size + 1, it)) }
                        contact.location?.let { headingsAndItemsAndTexts.add(makeEncounterLocation(headingsAndItemsAndTexts.size + 1, it, language)) }
                        contact.encounterType?.let { headingsAndItemsAndTexts.add(makeEncounterType(headingsAndItemsAndTexts.size + 1, it)) }

                        hesByContactId[contact.id].orEmpty().map { he -> addHealthCareElement(trn, he, 0, config, language) }

                        hesByContactId = hesByContactId.filterKeys { it != contact.id } // prevent re-using the same He for the next subcontact

                        services.filter { s -> s.tags.find { t -> t.code == "incapacity" } != null }.forEach { incapacityService ->
                            headingsAndItemsAndTexts.add(makeIncapacityItem(incapacityService, language))
                            incapacityService.content[language]?.documentId?.let { docId ->
                                createLinkToDocument(docId, healthcareParty, incapacityService, folder, language, config, parentContactId ?: transactionMFID, decryptor)
                            }
                        }
                        services.filter { s -> s.tags.find { t -> t.code == "physiotherapy" } != null }.forEach { kineService ->
                            specialPrescriptions.add(makeKinePrescriptionTransaction(kineService, language, decryptor, parentContactId ?: transactionMFID))
                        }
                        services.filter { s -> s.tags.find { t -> t.code == "medicalcares" } != null }.forEach { nurseService ->
                            specialPrescriptions.add(makeNursePrescriptionTransaction(nurseService, language, decryptor, parentContactId ?: transactionMFID))
                        }
                        services.filter { s -> isSummary(s) }.forEach { summaryService ->
                            summaries.add(makeSummaryTransaction(contact, summaryService, language))
                        }

                        // services
                        if (exportAsDocument && services.size == 1) {
                            services[0].content.values.forEach { doc ->
                                doc.stringValue?.let {
                                    headingsAndItemsAndTexts.add(LnkType().apply {
                                        type = CDLNKvalues.MULTIMEDIA
                                        mediatype = CDMEDIATYPEvalues.TEXT_PLAIN
                                        value = it.toByteArray(Charsets.UTF_8) })
                                }
                            }
                        } else {
                            services.forEach servicesLoop@{ svc ->
                                val specialTag = svc.tags.find { t -> listOf("incapacity", "physiotherapy", "medicalcares").contains(t.code) }
                                if (specialTag != null) return@servicesLoop
                                var forSeparateTransaction = false // documents are in separate transaction in *MF
                                svc.content.values.find { it.documentId != null }?.let {
                                    documents = documents.plus(Triple(it.documentId!!, svc, contact))
                                    forSeparateTransaction = true
                                }

                                // prescriptions are in separate transaction in *MF
                                // icure prescriptions have another tag (and in separate transaction in *MF)
                                svc.tags.find {
                                    (it.type == "CD-ITEM" && it.code == "treatment") ||
                                        (it.type == "ICURE" && it.code == "PRESC")
                                }?.let {
                                    pharmaceuticalPrescriptions = pharmaceuticalPrescriptions.plus(Pair(svc, contact))
                                    forSeparateTransaction = true
                                }
                                forSeparateTransaction = forSeparateTransaction || isSummary(svc)

                                if (!forSeparateTransaction) {
                                    val svcCdItem = svc.tags.firstOrNull { it.type == "CD-ITEM" }

                                    val cdItem = (svcCdItem?.code ?: defaultCdItemRef).let {
                                        if (it == "parameter") {
                                            svc.content.let { contents ->
                                                contents.entries.firstOrNull()?.value?.measureValue?.let { "parameter" }
                                                    ?: "clinical" // FIXME: change to clinical instead of technical because medinote doesnt support technical
                                            } // Change parameters to technicals if not real parameters
                                        } else {
                                            it
                                        }
                                    }

                                    val contents = svc.content.entries.flatMap {
                                        makeContent(it.key, it.value)?.let { c ->
                                            listOf(
                                                c.apply {
                                                    if (svcCdItem == null && texts.size > 0) {
                                                        if (!svc.label.isNullOrBlank() && svc.label != "<invalid>" && !texts.first().value.startsWith(svc.label!!)) {
                                                            texts.first().value = "${svc.label}: ${texts.first().value}"
                                                        }
                                                    }
                                                }
                                            )
                                        } ?: emptyList()
                                    } + codesToKmehr(svc.codes)
                                    if (contents.isNotEmpty()) {
                                        val mfId = svc.id
                                        createItemWithContent(svc, headingsAndItemsAndTexts.size + 1, cdItem, contents, "MF-ID", mfId)?.apply {
                                            this.ids.add(
                                                IDKMEHR().apply {
                                                    this.s = IDKMEHRschemes.LOCAL
                                                    this.sv = "1.0"
                                                    this.sl = "org.taktik.icure.label"
                                                    this.value = svc.label
                                                },
                                            )
                                            if (cdItem == "parameter") {
                                                svc.content.values.firstOrNull { it.measureValue != null }?.measureValue?.comment?.let { measureNote ->
                                                    this.contents.add(ContentType().apply { texts.add(TextType().apply { l = language; value = measureNote }) })
                                                }

                                                svc.tags.find { it.type == "CD-PARAMETER" }?.let {
                                                    this.cds.add(
                                                        CDITEM().apply {
                                                            s = CDITEMschemes.CD_PARAMETER
                                                            value = it.code
                                                        },
                                                    )
                                                }
                                                if (!svc.label.isNullOrEmpty()) {
                                                    this.cds.add(
                                                        CDITEM().apply {
                                                            s = CDITEMschemes.LOCAL
                                                            sl = "LOCAL-PARAMETER"
                                                            sv = "1.0"
                                                            dn = if (svc.comment == "" || svc.comment == null) {
                                                                svc.label
                                                            } else {
                                                                svc.comment
                                                            }
                                                            value = svc.label
                                                        },
                                                    )
                                                }
                                            }
                                            if (cdItem == "medication") {
                                                svc.content.values.find { it.medicationValue?.instructionForPatient != null }?.let {
                                                    this.posology = ItemType.Posology().apply {
                                                        text = TextType().apply { l = language; value = it.medicationValue!!.instructionForPatient }
                                                    }
                                                }
                                            }
                                            if (cdItem == "vaccine") {
                                                svc.content.values.firstOrNull { it.medicationValue != null }?.medicationValue?.batch?.let {
                                                    this.batch = it
                                                }
                                            }

                                            svc.comment?.let {
                                                (it != "") && it.let {
                                                    this.contents.add(ContentType().apply { texts.add(TextType().apply { l = language; value = it }) })
                                                }
                                            }
                                        }?.let {
                                            addHistoryLinkAndCacheService(it, svc.id, config)
                                            headingsAndItemsAndTexts.add(it)
                                        }
                                    }
                                }
                            }
                            parentContactId?.let { headingsAndItemsAndTexts.add(LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(it) }) }
                        }
                        val subContactsWithHealthElementId = contact.subContacts.filter { subContactWithHealthElementId ->
                            subContactWithHealthElementId.healthElementId != null && subContactWithHealthElementId.formId == subContact.formId
                        }
                        addServiceLinkToHealthElement(subContactsWithHealthElementId)
                    },
                )
            }
        }
        log.info("Exporting pharmaceutical prescriptions")
        pharmaceuticalPrescriptions.forEachIndexed { index, it ->
            progressor?.progress((1.0 * (index + contacts.size)) / (contacts.size + documents.size))
            val (svc, con) = it
            folder.transactions.add(
                TransactionType().apply {
                    ids.add(idKmehr(startIndex))
                    ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; sv = "1.0"; value = svc.id })
                    cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "pharmaceuticalprescription" })
                    (svc.modified ?: svc.created)?.let {
                        date = makeXGC(it)
                        time = makeXGC(it)
                    } ?: also {
                        date = config.date
                        time = makeXGC(0L)
                    }
                    (svc.responsible ?: healthcareParty.id).let {
                        author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
                    }
                    isIscomplete = true
                    isIsvalidated = true
                    recorddatetime = Utils.makeXGC(svc.modified, true)
                    val svcCdItem =
                        svc.tags.firstOrNull { it.type == "CD-ITEM" } // is treatement in topaz but should be medication in kmehr
                    val cdItem = "medication" // force medication
                    val contents = svc.content.entries.flatMap {
                        makeContent(it.key, it.value)?.let { c ->
                            listOf(
                                c.apply {
                                    if (svcCdItem == null && texts.size > 0) {
                                        if (svc.label != null) {
                                            texts.first().value = "${svc.label}: ${texts.first().value}"
                                        }
                                    }
                                },
                            )
                        } ?: emptyList()
                    }.toMutableList()
                    contents += codesToKmehr(svc.codes)
                    if (contents.isNotEmpty()) {
                       createItemWithContent(svc, headingsAndItemsAndTexts.size + 1, cdItem, contents, "MF-ID")?.apply {
                            this.ids.add(
                                IDKMEHR().apply {
                                    this.s = IDKMEHRschemes.LOCAL
                                    this.sv = "1.0"
                                    this.sl = "org.taktik.icure.label"
                                    this.value = svc.label
                                },
                            )
                            if (cdItem == "medication") {
                                svc.content.values.find { it.medicationValue?.instructionForPatient != null }?.let {
                                    this.posology = ItemType.Posology().apply {
                                        text = TextType().apply { l = language; value = it.medicationValue!!.instructionForPatient }
                                    }
                                }
                            }
                            svc.comment?.let {
                                (it != "") && it.let {
                                    this.contents.add(ContentType().apply { texts.add(TextType().apply { l = language; value = it }) })
                                }
                            }
                            addHistoryLinkAndCacheService(this, svc.id, config)
                            headingsAndItemsAndTexts.add(this)
                            svc.formId?.let {
                                this.lnks.add(LnkType().apply { type = CDLNKvalues.ISATTESTATIONOF; url = makeLnkUrl(it) })
                            }
                        }
                    }
                    // FIXME: prescriptions should be linked to medication with a ISATTESTATIONOF link but there is no such link in topaz
                    headingsAndItemsAndTexts.add(LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(con.id) })
                    addServiceLinkToHealthElement(con)
                },
            )
        }

        documents.forEachIndexed { index, it ->
            try {
                log.info("Exporting document $index/${documents.size}")
                progressor?.progress((1.0 * (index + contacts.size)) / (contacts.size + documents.size))
                val (docId, svc, con) = it
                folder.transactions.add(
                    TransactionType().apply {
                        ids.add(idKmehr(startIndex))
                        ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; sv = "1.0"; value = svc.id })
                        cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "note"; dn = svc.content[language]?.stringValue })
                        (svc.modified ?: svc.created)?.let {
                            date = makeXGC(it)
                            time = makeXGC(it)
                        } ?: also {
                            date = config.date
                            time = makeXGC(0L)
                        }
                        (svc.responsible ?: healthcareParty.id).let {
                            author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
                        }
                        isIscomplete = true
                        isIsvalidated = true
                        recorddatetime = Utils.makeXGC(svc.modified, true)
                        svc.comment?.let {
                            headingsAndItemsAndTexts.add(
                                TextType().apply {
                                    l = language
                                    value = svc.comment
                                },
                            )
                        }
                        documentLogic.getDocument(docId)?.let { d ->
                            documentLogic.getAndDecryptMainAttachment(d.id)?.let { headingsAndItemsAndTexts.add(makeMultimediaLnkType(d, it, decryptor)) }
                        }
                        headingsAndItemsAndTexts.add(LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(con.id) })
                    },
                )
            } catch (e: Exception) {
                log.error("Cannot export document ${it.first}")
            }
        }

        specialPrescriptions.forEach {
            folder.transactions.add(it)
        }

        summaries.forEach {
            folder.transactions.add(it)
        }

        renumberKmehrIds(folder)
        return folder
    }

    /**
     * Adds a Link to a Service in an ItemType in a HealthElement.
     * @param subContacts a List of SubContacts to extract the Services from.
     */
    private fun addServiceLinkToHealthElement(subContacts: List<SubContact>) {
        subContacts.forEach { subContact ->
            if (subContact.healthElementId != null) {
                subContact.services.forEach {
                    itemByServiceId[it.serviceId]?.lnks?.let { lnkType ->
                        val lnk = LnkType().apply {
                            type = CDLNKvalues.ISASERVICEFOR
                            // link should point to He.healthElementId and not He.id
                            subContact.healthElementId.let { heId ->
                                heById[heId]?.firstOrNull()?.let { he ->
                                    he.healthElementId?.let { id ->
                                        url = makeLnkUrl(id)
                                    }
                                }
                            }
                        }
                        if (lnkType.none { l -> (l.type == lnk.type) && (l.url == lnk.url) }) {
                            lnkType.add(lnk)
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a Link fom an ItemType in a HealthElement.
     * @param contact an ICure Contact.
     */
    private fun addServiceLinkToHealthElement(contact: Contact) {
        val subContactsByFormId = contact.subContacts.groupBy { it.formId }
        val subContactServicesByFormId = subContactsByFormId.mapValues {
            it.value.flatMap { subContact -> subContact.services }
        }
        contact.subContacts.forEach { subCon ->
            if (subCon.healthElementId != null) {
                subContactServicesByFormId[subCon.formId]?.forEach {
                    itemByServiceId[it.serviceId]?.lnks?.let { lnkTypes ->
                        val lnk = LnkType().apply {
                            type = CDLNKvalues.ISASERVICEFOR
                            // link should point to He.healthElementId and not He.id
                            subCon.healthElementId.let { heId ->
                                heById[heId]?.firstOrNull()?.let { healthElement ->
                                    url = makeLnkUrl(healthElement.healthElementId ?: healthElement.id)
                                }
                            }
                        }
                        if (lnkTypes.none { l -> (l.type == lnk.type) && (l.url == lnk.url) }) {
                            lnkTypes.add(lnk)
                        }
                    }
                }
            }
        }
    }

    /**
     * Associates an ItemType to a Service by their id and adds a new LnkType to the item.
     * @param item the ItemType.
     * @param serviceId the Service id.
     * @param config the Config.
     */
    private fun addHistoryLinkAndCacheService(item: ItemType, serviceId: String, config: Config) {
        if (config.format != Config.Format.PMF) {
            itemByServiceId[serviceId]?.let {
                getItemMFID(it)?.let {
                    item.lnks.add(
                        LnkType().apply {
                            type = CDLNKvalues.ISANEWVERSIONOF; url = makeLnkUrl(it)
                        },
                    )
                }
            }
        }
        itemByServiceId[serviceId] = item
    }

    /**
     * Creates a Transaction with nursing type and information and document taken from the Service passed as
     * parameter.
     * @param service the iCure Service.
     * @param language the language of the content.
     * @param decryptor the Decryptor for the document.
     * @param transactionMfId the id of the transaction.
     * @return a TransactionType.
     */
    private suspend fun makeNursePrescriptionTransaction(service: Service, language: String, decryptor: AsyncDecrypt?, transactionMfId: String): TransactionType =
        makeSpecialPrescriptionTransaction(service, "nursing", language, decryptor, transactionMfId)

    /**
     * Creates a Transaction with physiotherapy type and information and document taken from the Service passed as
     * parameter.
     * @param service the iCure Service.
     * @param language the language of the content.
     * @param decryptor the Decryptor for the document.
     * @param transactionMfId the id of the transaction.
     * @return a TransactionType.
     */
    private suspend fun makeKinePrescriptionTransaction(service: Service, language: String, decryptor: AsyncDecrypt?, transactionMfId: String): TransactionType =
        makeSpecialPrescriptionTransaction(service, "physiotherapy", language, decryptor, transactionMfId)

    /**
     * Creates a Transaction with the specified type and information and document taken from the Service passed as
     * parameter.
     * @param service the iCure Service.
     * @param transactionType the type of the transaction.
     * @param language the language of the content.
     * @param decryptor the Decryptor for the document.
     * @param transactionMfId the id of the transaction.
     * @return a TransactionType.
     */
    private suspend fun makeSpecialPrescriptionTransaction(
        service: Service,
        transactionType: String,
        language: String,
        decryptor: AsyncDecrypt?,
        transactionMfId: String
    ): TransactionType = TransactionType().apply {
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; value = "1" })
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; value = service.id })
            cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "prescription" })
            cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION_TYPE; value = transactionType })
            (service.modified ?: service.created)?.let {
                date = makeXGC(it)
                time = makeXGC(it)
            }
            service.responsible?.let {
                author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
            }
            service.created?.let {
                recorddatetime = Utils.makeXGC(it)
            }
            isIscomplete = true
            isIsvalidated = true
            LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(transactionMfId) }.also { headingsAndItemsAndTexts.add(it) }

            service.content[language]?.documentId?.let { documentId ->
                try {
                    documentLogic.getDocument(documentId)?.let { d -> documentLogic.getAndDecryptMainAttachment(d.id)?.let { headingsAndItemsAndTexts.add(makeMultimediaLnkType(d, it, decryptor)) } }
                } catch (e: Exception) {
                    log.error("Cannot export document $documentId")
                }
            }
        }

    /**
     * Creates an ItemType from an iCure Service with a MS-INCAPACITYTYPE tag.
     * @param service the iCure Service.
     * @param language the language of the Content.
     * @param index the index of the KmEHR.
     * @return the ItemType.
     */
    private fun makeIncapacityItem(service: Service, language: String, index: Number = 0): ItemType {
        fun getCompoundValueContent(label: String) =
            service.content[language]?.compoundValue?.firstOrNull { it.label == label }?.content?.values?.firstOrNull()
        fun getCompoundValueTag(label: String, tagType: String) =
            service.content[language]?.compoundValue?.firstOrNull { it.label == label }?.tags?.find { it.type == tagType }
        return ItemType().apply {
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; value = index.toString() })
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; sv = kmehrConfig.kmehrVersion; value = service.id })
            cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = "incapacity" })
            service.tags.firstOrNull { it.type == "MS-INCAPACITYTYPE" }?.code?.let { incapacityType ->
                cds.add(CDITEM().apply { s = CDITEMschemes.LOCAL; value = incapacityType; sl = "PMF-PARAMETER"; l = language; dn = "incapacity type" })
            }

            contents.add(
                ContentType().apply {
                    incapacity = IncapacityType().apply {
                        (
                            service.tags.find { it.type == "CD-INCAPACITY" }
                                ?: service.tags.find { it.type == "CD-INCAPACITY-EXT" }
                            )?.let { incapacityTag ->
                            cds.add(
                                CDINCAPACITY().apply {
                                    try {
                                        value = CDINCAPACITYvalues.fromValue(incapacityTag.code)
                                    } catch (e: IllegalArgumentException) {
                                        // TODO ignored for now should be other
                                    }
                                },
                            )
                            getCompoundValueContent("Percentage")?.numberValue?.let {
                                percentage = it.toBigDecimal()
                            }
                        }
                        (
                            getCompoundValueTag("Reason", "CD-INCAPACITYREASON")?.code
                                ?: getCompoundValueTag("Reason", "CD-INCAPACITYREASON-EXT")?.code
                            )
                            ?.let { reasonValue ->
                                incapacityreason = IncapacityreasonType().apply {
                                    cd = CDINCAPACITYREASON().apply {
                                        value = try {
                                            CDINCAPACITYREASONvalues.fromValue(reasonValue)
                                        } catch (e: IllegalArgumentException) {
                                            CDINCAPACITYREASONvalues.fromValue("other")
                                        }
                                    }
                                }
                            }
                        getCompoundValueTag("Outing", "MS-INCAPACITYOUTING")?.code?.let { outingCode ->
                            isOutofhomeallowed = when (outingCode) {
                                "allowed", "notrecommended" -> true
                                else -> false
                            }
                        }

                        getCompoundValueContent("Diagnosis")?.stringValue?.let { diagnosisText ->
                            texts.add(
                                TextType().apply {
                                    l = language
                                    value = diagnosisText
                                },
                            )
                        }
                    }
                },
            )

            lifecycle = LifecycleType().apply {
                cd = CDLIFECYCLE().apply {
                    value = service.tags.find { t -> t.type == "CD-LIFECYCLE" }?.let { CDLIFECYCLEvalues.fromValue(it.code) }
                        ?: CDLIFECYCLEvalues.ACTIVE
                }
            }
            isIsrelevant = true
            getCompoundValueContent("StartDate")?.instantValue?.let {
                beginmoment = makeMomentType(it, precision = ChronoUnit.DAYS)
            }
            getCompoundValueContent("EndDate")?.instantValue?.let {
                endmoment = makeMomentType(it, precision = ChronoUnit.DAYS)
            }
            service.modified?.let {
                recorddatetime = Utils.makeXGC(it, true)
            }
        }
    }

    /**
     * Checks if a Service is a summary by checking the label and the tags.
     * @param s the iCure Service.
     * @return true if it is a summary, false otherwise.
     */
    private fun isSummary(s: Service) = s.label == "Summary" && s.tags.find { t -> t.code == "summary" } != null

    /**
     * Creates a TransactionType from an iCure Service with a summaryContent.
     * @param contact the iCure Contact.
     * @param service the iCure Service.
     * @param language the language of the Content.
     * @return the TransactionType.
     */
    private suspend fun makeSummaryTransaction(contact: Contact, service: Service, language: String): TransactionType {
        fun getCompoundValueContent(service: Service, label: String) =
            service.content[language]?.compoundValue?.firstOrNull { it.label == label }?.content?.get(language)
        fun getCompoundValueTag(service: Service, label: String, tagType: String) =
            service.content[language]?.compoundValue?.firstOrNull { it.label == label }?.tags?.find { it.type == tagType }

        return TransactionType().apply {
            val title = getCompoundValueContent(service, "SummaryTitle")
            val content = getCompoundValueContent(service, "SummaryContent")
            val cdMediaType = getCompoundValueTag(service, "SummaryContent", "CD-MEDIATYPE")
            val mediaType = try {
                CDMEDIATYPEvalues.fromValue(cdMediaType?.code)
            } catch (ignored: IllegalArgumentException) {
                null
            }

            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; value = "1" })
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; value = service.id })
            cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "note"; dn = title?.stringValue })
            (service.modified ?: service.created)?.let {
                date = makeXGC(it)
                time = makeXGC(it)
            }
            service.responsible?.let {
                author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
            }
            service.created?.let {
                recorddatetime = Utils.makeXGC(it)
            }
            isIscomplete = true
            isIsvalidated = true

            headingsAndItemsAndTexts.add(LnkType().apply { type = CDLNKvalues.MULTIMEDIA; mediatype = mediaType; value = content?.stringValue?.toByteArray() })
            headingsAndItemsAndTexts.add(LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(contact.id) })
        }
    }

    /**
     * Adds a TransactionType to the folder passed as parameter. It also adds to it a LnkType to the iCure Document
     * which id is passed as parameter.
     * @param documentId the iCure Document id to add to the transaction.
     * @param healthcareParty the HCP responsible for the export.
     * @param service the Service related to the Document.
     * @param folder the FolderType.
     * @param language the language of the content.
     * @param config the Config.
     * @param transactionMfId the ID of the transaction.
     * @param decryptor the Decryptor for the Document.
     */
    private suspend fun createLinkToDocument(
        documentId: String,
        healthcareParty: HealthcareParty,
        service: Service,
        folder: FolderType,
        language: String,
        config: Config,
        transactionMfId: String,
        decryptor: AsyncDecrypt?
    ) {
        try {
            folder.transactions.add(
                TransactionType().apply {
                    ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; sv = "1.0"; value = service.id })
                    cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "note" })
                    (service.modified ?: service.created)?.let {
                        date = makeXGC(it)
                        time = makeXGC(it)
                    }
                        ?: also {
                            date = config.date
                            time = makeXGC(0L)
                        }
                    (service.responsible ?: healthcareParty.id).let {
                        author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
                    }
                    isIscomplete = true
                    isIsvalidated = true
                    recorddatetime = Utils.makeXGC(service.modified, true)
                    service.comment?.let {
                        headingsAndItemsAndTexts.add(
                            TextType().apply {
                                l = language
                                value = service.comment
                            },
                        )
                    }
                    documentLogic.getDocument(documentId)?.let { d ->
                        documentLogic.getAndDecryptMainAttachment(d.id)?.let {
                            val element = makeMultimediaLnkType(d, it, decryptor)
                            headingsAndItemsAndTexts.add(element)
                        }
                    }
                    LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(transactionMfId) }.also { headingsAndItemsAndTexts.add(it) }
                },
            )
        } catch (e: Exception) {
            log.error("Cannot export document $documentId")
        }
    }

    /**
     * Creates a LnkType element from an iCure Document. It decrypts the Document if has a decryptor.
     * @param document the iCure Document.
     * @param attachment the Document attachment.
     * @param decryptor the decryptor.
     * @return a LnkType.
     */
    private suspend fun makeMultimediaLnkType(
        document: Document,
        attachment: ByteArray,
        decryptor: AsyncDecrypt?,
    ): LnkType {
        val data = if (document.encryptionKeys.isNotEmpty() && decryptor != null) {
            decryptor.decrypt(
                listOf(documentMapper.map(document).copy(encryptedAttachment = documentLogic.getAndDecryptMainAttachment(document.id))),
                DocumentDto::class.java
            ).firstOrNull()?.decryptedAttachment
                ?: attachment
        } else {
            attachment
        }
        return LnkType().apply { type = CDLNKvalues.MULTIMEDIA; mediatype = documentMediaType(document); value = data }
    }

    /**
     * Creates an ItemType of type of encounterdatetime from a Fuzzy Date.
     * @param index the ItemType index.
     * @param yyyymmddhhmmss the Fuzzy date.
     * @return an ItemType.
     */
    private fun makeEncounterDateTime(index: Int, yyyymmddhhmmss: Long): ItemType =
        ItemType().apply {
            ids.add(idKmehr(index))
            cds.add(cdItem("encounterdatetime"))
            contents.add(
                ContentType().apply {
                    date = makeXMLGregorianCalendarFromFuzzyLong(yyyymmddhhmmss)
                    time = makeXMLGregorianCalendarFromFuzzyLong(yyyymmddhhmmss)?.apply {
                        if (hour == DatatypeConstants.FIELD_UNDEFINED) {
                            hour = 0
                        }
                        if (minute == DatatypeConstants.FIELD_UNDEFINED) {
                            minute = 0
                        }
                        if (second == DatatypeConstants.FIELD_UNDEFINED) {
                            second = 0
                        }
                    }
                },
            )
        }

    /**
     * Creates an ItemType of type of encounterlocation with a location content.
     * @param index the ItemType index.
     * @param location the location.
     * @param language the language of the content.
     * @return an ItemType.
     */
    private fun makeEncounterLocation(index: Int, location: String, language: String): ItemType =
        ItemType().apply {
            ids.add(idKmehr(index))
            cds.add(cdItem("encounterlocation"))
            contents.add(
                ContentType().apply {
                    texts.add(TextType().apply { l = language; value = location })
                }
            )
        }

    /**
     * Creates an ItemType of type of encountertype with a Code.
     * @param index the ItemType index.
     * @param encounterType the CodeStub that defines the encounter type.
     * @return an ItemType.
     */
    private fun makeEncounterType(index: Int, encounterType: CodeStub): ItemType =
        ItemType().apply {
            ids.add(idKmehr(index))
            cds.add(cdItem("encountertype"))
            contents.add(
                ContentType().apply {
                    cds.add(CDCONTENT().apply { s = CDCONTENTschemes.CD_ENCOUNTER; value = encounterType.code })
                }
            )
        }

    /**
     * Extracts all the partnerships for a Patient and creates an ItemType of type "contactperson" for each one of them.
     * @param startIndex the last KmEHR index used.
     * @param pat the iCure Patient.
     * @param config the Config.
     * @return a List of ItemType.
     */
    private suspend fun makeContactPeople(startIndex: Int, pat: Patient, config: Config): List<ItemType> {
        val partnersById: Map<String, Patient> = patientLogic.getPatients(pat.partnerships.mapNotNull { it.partnerId })
            .filterNotNull().toList().associateBy { partner -> partner.id }

        return pat.partnerships
            .filter { it.partnerId != null }
            .mapIndexed { i, partnership ->
                partnersById[partnership.partnerId]?.let { partner ->
                    ItemType().apply {
                        ids.add(idKmehr(startIndex + i))
                        ids.add(localIdKmehrElement(startIndex + i, config))
                        cds.add(cdItem("contactperson"))
                        cds.add(CDITEM().apply { s(CDITEMschemes.CD_CONTACT_PERSON); value = partnership.otherToMeRelationshipDescription })
                        contents.add(ContentType().apply { person = makePerson(partner, config) })
                    }
                }
            }.filterNotNull()
    }

    /**
     * Creates an ItemType of type "gmdmanager" for the HCP passed as parameter.
     * @param itemIndex the ItemType index.
     * @param config the Config.
     * @param hcp the HCP.
     * @param period the ReferralPeriod.
     * @return an ItemType.
     */
    private suspend fun makeGmdManager(itemIndex: Int, config: Config, hcp: HealthcareParty, period: ReferralPeriod): ItemType? =
        ItemType().apply {
            ids.add(idKmehr(itemIndex))
            ids.add(localIdKmehrElement(itemIndex, config))
            cds.add(cdItem("gmdmanager"))
            contents.add(ContentType().apply { hcparty = createParty(hcp) })
            beginmoment = period.startDate?.let { makeMomentType(it, precision = ChronoUnit.DAYS) }
            recorddatetime = period.startDate?.let { makeXmlGregorianCalendar(it) } // should be the modification date, but it's not present
        }.takeIf { it.contents.first().hcparty.ids.filter { hcpId -> hcpId.s == IDHCPARTYschemes.ID_HCPARTY }.size == 1 }

    /**
     * Gets the insurance from the Insurability passed as parameter and converts it to an ItemType.
     * @param itemIndex the index of the ItemType to create.
     * @param config the Config for the Local KmEHR id.
     * @param insurability the Insurability.
     * @return an ItemType.
     */
    private suspend fun makeInsurancyStatus(itemIndex: Int, config: Config, insurability: Insurability?): ItemType? {
        val insStatus = ItemType().apply {
            ids.add(idKmehr(itemIndex))
            ids.add(localIdKmehrElement(itemIndex, config))
            cds.add(cdItem("insurancystatus"))
            if (insurability?.insuranceId?.isBlank() == false) {
                try {
                    insuranceLogic.getInsurance(insurability.insuranceId!!)?.let {
                        if (it.code != null && it.code!!.length >= 3) {
                            contents.add(
                                ContentType().apply {
                                    insurance = InsuranceType().apply {
                                        id = IDINSURANCE().apply { s = IDINSURANCEschemes.ID_INSURANCE; value = it.code!!.substring(0, 3); }
                                        membership = insurability.identificationNumber ?: ""
                                        insurability.parameters["tc1"]?.let { tc ->
                                            cg1 = tc
                                            insurability.parameters["tc2"]?.let { cg2 = tc }
                                        }
                                    }
                                }
                            )
                        }
                    }
                } catch (ignored: DocumentNotFoundException) { }
            }
        }
        return insStatus.takeIf { insStatus.contents.size > 0 }
    }

    /**
     * Creates a CD-ITEM with the value passed as paramter.
     * @param v the value.
     * @return a CDITEM.
     */
    private fun cdItem(v: String): CDITEM =
        CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = v }

    /**
     * Finds the HCP with the latest referral period for a Patient.
     * @param pat the iCure Patient.
     * @return a Pair containing the HCP and the ReferralPeriod.
     */
    private suspend fun getLastGmdManager(pat: Patient): Pair<HealthcareParty?, ReferralPeriod?> {
        val isActive: (ReferralPeriod) -> Boolean = { r -> r.startDate?.isBefore(Instant.now()) == true && null == r.endDate }
        val gmdRelationship = pat.patientHealthCareParties.find { it.referralPeriods.any(isActive) }
            ?: return Pair(null, null)
        val gmd = gmdRelationship.healthcarePartyId?.let { healthcarePartyLogic.getHealthcareParty(it) }
        return Pair(gmd, gmdRelationship.referralPeriods.find(isActive))
    }

    /**
     * Gets and HCP by id and creates an HcpartyType from it.
     * @param hcpId the HCP id.
     * @param cds the CDHPARTY to add to the HcpartyType.
     * @return the HcpartyType.
     */
    private suspend fun checkHcpAndCreateCorrespondingPartyType(hcpId: String, cds: List<CDHCPARTY>? = emptyList()): HcpartyType? =
        healthcarePartyLogic.getHealthcareParty(hcpId)?.let { createParty(it, cds) }

    private fun codesToKmehr(codes: Set<CodeStub>): ContentType {
        return ContentType().apply {
            cds.addAll(
                codes.map { code ->
                    when {
                        code.type == "ICPC" -> CDCONTENT().apply { s = CDCONTENTschemes.ICPC; sv = code.version; value = code.code }
                        code.type == "ICD" -> CDCONTENT().apply { s = CDCONTENTschemes.ICD; sv = code.version; value = code.code }
                        code.type == "CD-ATC" -> CDCONTENT().apply { s = CDCONTENTschemes.CD_ATC; sv = code.version; value = code.code }
                        code.type == "CD-PATIENTWILL" -> CDCONTENT().apply { s = CDCONTENTschemes.CD_PATIENTWILL; sv = code.version; value = code.code }
                        code.type == "BE-THESAURUS" -> CDCONTENT().apply { s = CDCONTENTschemes.CD_CLINICAL; sv = code.version; value = code.code } // FIXME: no spec for version can be found regarding thesaurus
                        code.type == "BE-THESAURUS-PROCEDURES" -> CDCONTENT().apply {
                            // FIXME: this is specific to pricare and icure, what format should we use ?
                            s = CDCONTENTschemes.LOCAL
                            sl = "BE-THESAURUS-PROCEDURES"
                            sv = code.version
                            value = "${code.code}"
                        }
                        code.type == "CD-VACCINEINDICATION" -> CDCONTENT().apply { s = CDCONTENTschemes.CD_VACCINEINDICATION; sv = code.version; value = code.code }
                        code.type?.startsWith("MS-EXTRADATA") ?: false -> CDCONTENT().apply { s = CDCONTENTschemes.LOCAL; sv = code.version; sl = code.type; dn = code.type; value = code.code }
                        else -> CDCONTENT().apply {
                            s = CDCONTENTschemes.LOCAL
                            sl = "ICURE.MEDICALCODEID"
                            dn = "ICURE.MEDICALCODEID"
                            sv = code.version ?: "1.0"
                            value = code.code
                        }
                    }
                },
            )
        }
    }

    /**
     * Checks if the HE passed as parameter is the new version of an existing one.
     * Since HEs versions have same healthElementId, if the HE is the oldest with this ID, it's not a new version.
     * @param he the HealthElement.
     * @return true if it is the new version, false otherwise.
     */
    private fun isHeANewVersionOf(he: HealthElement): Boolean =
        oldestHeByHeId[he.healthElementId]?.id?.let {
            return it != he.id
        } ?: false

    /**
     * Converts the HealthElement passed as parameter to an ItemType and adds it to the TransactionType passed as
     * parameter.
     * @param trn the TransactionType.
     * @param eds the HealthElement.
     * @param itemIndex the index of the new ItemType.
     * @param config the Config.
     * @param language the language of the ContentType.
     * @return the next index to use.
     */
    private fun addHealthCareElement(trn: TransactionType, eds: HealthElement, itemIndex: Int = 0, config: Config, language: String): Int =
        try {
            val content = listOf(
                ContentType().apply {
                    texts.add(TextType().apply { l = language; value = eds.descr })
                },
                ContentType().apply {
                    cds.addAll(codesToKmehr(eds.codes).cds)
                },
            )
            val itemType = eds.tags.find { it.type == "CD-ITEM" }?.code ?: "healthcareelement"
            createItemWithContent(eds, itemIndex, itemType, content, "MF-ID")?.let {
                if (isHeANewVersionOf(eds) && config.format != Config.Format.PMF && eds.healthElementId != null) { // no versioning in PMF
                    it.lnks.add(
                        LnkType().apply {
                            type = CDLNKvalues.ISANEWVERSIONOF; url = makeLnkUrl(eds.healthElementId!!)
                        },
                    )
                }
                if (!(config.format == Config.Format.PMF && !it.isIsrelevant && it.lifecycle.cd.value == CDLIFECYCLEvalues.INACTIVE)) {
                    // inactive irrelevant items should not be exported in PMF
                    trn.headingsAndItemsAndTexts.add(it)
                }
                itemIndex+1
            } ?: itemIndex
        } catch (e: Exception) {
            log.error("Unexpected error", e)
            itemIndex
        }

    /**
     * Retrieves all the HealthElements for an HCP and a Patient through its Secret Foreign Keys, filtered by description
     * @param hcp the HCP responsible for the export.
     * @param sfks the Patient Secret Foreign Keys.
     * @param config the Config.
     * @return a List of HealthElements.
     */
    private suspend fun getHealthElements(hcp: HealthcareParty, sfks: List<String>, config: Config): List<HealthElement> {
        val res = (healthElementLogic.listHealthElementsByHcPartyAndSecretPatientKeys(hcp.id, sfks).toList() +
                (hcp.parentId?.let { parentId ->
                    healthElementLogic.listHealthElementsByHcPartyAndSecretPatientKeys(parentId, sfks).toList()
                } ?: emptyList())).distinctBy { it.id }
        return excludeHealthElementsForPMF(
            res.filterNot {
                it.descr?.matches("INBOX|Etat général.*|Algemeen toestand.*".toRegex()) ?: false
            }.toList(),
            config
        )
    }

    /**
     * Given a List of HealthElements, filters out all the ones that are not active and not relevant if the Config
     * format is PMF.
     * @param heList the List of HealthElements.
     * @param config the Config.
     * @return the filtered List of HealthElements.
     */
    private fun excludeHealthElementsForPMF(heList: List<HealthElement>, config: Config): List<HealthElement> =
        // PMF = all active items + all relevant inactive items
        if (config.format == Config.Format.PMF) {
            heList.filter {
                (it.endOfLife == null || it.endOfLife == 0L) && // not deleted
                    ((it.tags.any { t -> t.type == "CD-LIFECYCLE" && t.code == "active" } || // is tagged active
                                ((it.status and 0x01) == 0) // or is status active
                            ) ||
                            ((it.status and 0x10) == 0)) // is status relevant
            }
        } else heList

    /**
     * Finds all the contact for a Patient, give the Secret Foreing Keys, and a HealthcareParty and its parent.
     * @param hcp the HCP.
     * @param sfks a List of Secret Foreign Keys.
     * @return a List of Contacts.
     */
    private suspend fun getAllContacts(hcp: HealthcareParty, sfks: List<String>): List<Contact> {
        val res = contactLogic.listContactsByHCPartyAndPatient(hcp.id, sfks.toList()).toList() +
                (hcp.parentId?.let { parentId ->
                    contactLogic.listContactsByHCPartyAndPatient(parentId, sfks.toList()).toList()
                } ?: emptyList())
        return res.distinctBy { it.id }
    }

    /**
     * Given a List of ICureDocuments, filters out all the ones which Confidentiality is Secret or their Visibility is
     * "masked from summary".
     * @param items the List of Items.
     * @return the List of filtered items.
     */
    private fun <T : ICureDocument<String>> getNonConfidentialItems(items: List<T>): List<T> =
        items.filter { s ->
            null == s.tags.find { it.type == "org.taktik.icure.entities.embed.Confidentiality" && it.code == "secret" } &&
                null == s.codes.find { it.type == "org.taktik.icure.entities.embed.Visibility" && it.code == "maskedfromsummary" }
        }

    private fun makeLnkUrl(id: String): String = "//item[id[@SL=\"MF-ID\" and .=\"${id}\"]]"

    /**
     * Creates a Transaction with the specified type and information and document taken from the Contact and Form passed as
     * parameter.
     * @param contact the iCure Contact.
     * @param form the iCure Form.
     * @param cdTransactionType the type of the transaction.
     * @param data additional data to add to the Transaction header.
     * @return a TransactionType.
     */
    private suspend fun makeSpecialPrescriptionTransaction(contact: Contact, form: Form, cdTransactionType: String, data: ByteArray): TransactionType =
        // for kine and nurse prescriptions
        TransactionType().apply {
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; value = "1" })
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "MF-ID"; value = form.id })
            cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; value = "prescription" })
            cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION_TYPE; value = cdTransactionType })
            contact.modified?.let {
                date = makeXGC(it)
                time = makeXGC(it)
            }
            contact.responsible?.let {
                author = AuthorType().apply { hcparties.add(checkHcpAndCreateCorrespondingPartyType(it, emptyList())) }
            }
            recorddatetime = Utils.makeXGC(contact.created) // TODO: maybe should take form date instead of contact date
            isIscomplete = true
            isIsvalidated = true

            headingsAndItemsAndTexts.add(
                LnkType().apply { type = CDLNKvalues.MULTIMEDIA; value = data },
            )
            headingsAndItemsAndTexts.add(
                LnkType().apply { type = CDLNKvalues.ISACHILDOF; url = makeLnkUrl(contact.id) },
            )
        }

    /**
     * Creates a Transaction related to the prescription of a physiotherapy.
     * @param contact the iCure Contact.
     * @param subCon the SubContacts.
     * @param form the iCure Form.
     * @param language the language of the Content.
     * @return a TransactionType.
     */
    suspend fun makeKinePrescriptionTransaction(contact: Contact, subCon: SubContact, form: Form, language: String): TransactionType {
        val data = renderKinePrescriptionTemplate(contact, subCon, language)
        return makeSpecialPrescriptionTransaction(contact, form, "physiotherapy", data)
    }

    /**
     * Creates a Transaction related to the prescription of nursing.
     * @param contact the iCure Contact.
     * @param form the iCure Form.
     * @param language the language of the Content.
     * @return a TransactionType.
     */
    suspend fun makeNursePrescriptionTransaction(contact: Contact, subCon: SubContact, form: Form, language: String): TransactionType {
        val data = renderNursePrescriptionTemplate(contact, subCon, language)
        return makeSpecialPrescriptionTransaction(contact, form, "nursing", data)
    }


    /**
     * Creates a prescription template for nursing.
     * @param contact the iCure Contact.
     * @param subCon the SubContacts.
     * @param language the language of the Content.
     * @return the template as ByteArray
     */
    private fun renderNursePrescriptionTemplate(contact: Contact, subCon: SubContact, language: String): ByteArray {
        // TODO: not working yet, template and mapping need to be done

        val mf: MustacheFactory = DefaultMustacheFactory()
        val m: Mustache = mf.compile("NursePrescription.mustache")
        val writer = StringWriter()

        val servKeys = mapOf(
            "Communication par courrier" to "contactMailPreference",
            "Communication par téléphone" to "contactPhonePreference",
            "Communication autre" to "contactOtherDetails", // NOTE: contactOtherPreference bit is not really relevant since text is filled
        )
        val servMap = contact.services
            .filter { serv -> subCon.services.map { it.serviceId }.contains(serv.id) }
            .associateBy({ it.label }, { it })

        fun getServiceValue(serv: Service?): String {
            return serv?.content?.let {
                it[language] ?: it.values.firstOrNull()
            }?.let {
                it.stringValue
                    ?: it.booleanValue?.let { b -> if (b) "X" else "" }
                    ?: it.numberValue?.toString()
                    ?: it.measureValue?.let { n -> n.value.toString() }
            } ?: ""
        }

        val keyServ = servKeys.keys.associateBy({ servKeys[it] }, { getServiceValue(servMap[it]) })

        val dat = mapOf(
            "nurse" to keyServ,
            "pat" to mapOf(
                "lname" to "testLname",
                "fname" to "testFname",
            )
        )

        m.execute(writer, dat)

        val html: String = writer.toString()
        println(html)
        return html.toByteArray()
    }

    /**
     * Gets the MF-ID from an ItemType.
     * @param item the ItemType.
     * @return the MF-ID, if present.
     */
    private fun getItemMFID(item: ItemType): String? =
        item.ids.find { it.s == IDKMEHRschemes.LOCAL && it.sl == "MF-ID" }?.value

    /**
     * Creates a prescription template for physiotherapy.
     * @param contact the iCure Contact.
     * @param subCon the SubContacts.
     * @param language the language of the Content.
     * @return the template as ByteArray
     */
    private fun renderKinePrescriptionTemplate(contact: Contact, subCon: SubContact, language: String): ByteArray {
        // TODO: not working yet, template and mapping need to be done

        val mf: MustacheFactory = DefaultMustacheFactory()
        val m: Mustache = mf.compile("KinePrescription.mustache")
        val writer = StringWriter()

        val servkeys = mapOf(
            "Prescription de kinésithérapie" to "opinionRequest",
            "Le patient ne peut se déplacer" to "PatientCannotLeaveHome",
            "Demande d'avis consultatif kiné" to "opinionRequest",
            "Mobilisation" to "fMobilisation",
            "Massage" to "fMasg",
            "Thermotherapie" to "fThermotherapy",
            "Kiné respiratoire tapotements" to "fTaping",
            "Localisation" to "localisation",
            "Drainage lymphatique" to "fDrain",
            "Gymnastique" to "fGym",
            "Nombre de séances" to "numSession",
            "Fréquence" to "freq",
            "Code d'intervention" to "surgicalInterventionCode",
            "Diagnostic" to "diagnostic",
            "Imagerie kiné" to "imageryAvailable",
            "Autre avis kiné" to "importantMedicalInfo", // TODO: not sure this match
            "Biologie kiné" to "biologyAvailable",
            "Avis spécialisé kiné" to "specialisedOpinionAvailable", // TODO: not sure this match
            "Evolution pendant tt" to "feedbackRequiredDuring",
            "Evolution fin tt" to "feedbackRequiredAtTheEnd",
            "Communication par courrier" to "contactMailPreference",
            "Communication par téléphone" to "contactPhonePreference",
            "Communication autre" to "contactOtherDetails", // NOTE: contactOtherPreference bit is not really relevant since text is filled
        )
        val servMap = contact.services
            .filter { serv -> subCon.services.map { it.serviceId }.contains(serv.id) }
            .associateBy({ it.label }, { it })

        fun getServiceValue(serv: Service?): String {
            return serv?.content?.let {
                it[language] ?: it.values.firstOrNull()
            }?.let {
                it.stringValue
                    ?: it.booleanValue?.let { b ->  if (b) "X" else "" }
                    ?: it.numberValue?.toString()
                    ?: it.measureValue?.let { m -> m.value.toString() }
            } ?: ""
        }

        val keyserv = servkeys.keys.associateBy({ servkeys[it] }, { getServiceValue(servMap[it]) })

        val dat = mapOf(
            "kine" to keyserv,
            "pat" to mapOf(
                "lname" to "testLname",
                "fname" to "testFname",
            )
        )

        m.execute(writer, dat)

        val html: String = writer.toString()
        println(html)
        return html.toByteArray()
    }

    /**
     * Recalculates the indexes of the items in a FolderType.
     * @param folder the FolderType.
     */
    private fun renumberKmehrIds(folder: FolderType) {
        folder.transactions.fold(1) { transactionIndex, it ->
            val newTransactionIndex = it.ids.find { it.s == IDKMEHRschemes.ID_KMEHR }?.let { id ->
                id.value = transactionIndex.toString()
                transactionIndex + 1
            } ?: transactionIndex
            it.headingsAndItemsAndTexts.fold(1) { itemIndex, itt ->
                try {
                    itt as ItemType // can fail with exception when not an Item
                    itt.ids.find { id -> id.s == IDKMEHRschemes.ID_KMEHR }?.let { id ->
                        id.value = itemIndex.toString()
                        itemIndex + 1
                    } ?: itemIndex
                } catch (ex: java.lang.ClassCastException) {
                    itemIndex
                }
            }
            newTransactionIndex
        }
    }
}
