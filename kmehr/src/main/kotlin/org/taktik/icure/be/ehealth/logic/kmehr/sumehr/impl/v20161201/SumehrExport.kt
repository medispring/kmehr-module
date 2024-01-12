/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.sumehr.impl.v20161201

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Profile
import org.taktik.icure.asynclogic.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.*
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHR
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.*
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.be.ehealth.logic.kmehr.emitMessage
import org.taktik.icure.be.ehealth.logic.kmehr.getSignature
import org.taktik.icure.be.ehealth.logic.kmehr.v20161201.KmehrExport
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.constants.ServiceStatus
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.ICureDocument
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Partnership
import org.taktik.icure.entities.embed.PatientHealthCareParty
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.services.external.api.AsyncDecrypt
import org.taktik.icure.services.external.rest.v1.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v1.mapper.HealthElementMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.ServiceMapper
import org.taktik.icure.utils.FuzzyValues
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Profile("kmehr")
@org.springframework.stereotype.Service("sumehrExportV2")
class SumehrExport(
    codeLogic: CodeLogic,
    documentLogic: DocumentLogic,
    kmehrConfiguration: KmehrConfiguration,
    private val serviceMapper: ServiceMapper,
    private val healthElementMapper: HealthElementMapper,
    val patientLogic: PatientLogic,
    private val healthElementLogic: HealthElementLogic,
    val healthcarePartyLogic: HealthcarePartyLogic,
    val contactLogic: ContactLogic) : KmehrExport(
    codeLogic,
    documentLogic,
    kmehrConfiguration
) {
    override val log = LogFactory.getLog(SumehrExport::class.java)

    /**
     * This method calculates the MD5 signature for a set of Services and HealthElements. Both can be passed as parameters
     * or retrieved from the database. In this case, all the Services and HealthElements retrieved are the ones related
     * to the HCP passed as parameter and to the Patients whose Secret Foreign Keys are passed as parameter.
     * The MD5 signature is calculated by sorting and joining in a string the IDs of all the retrieved entities.
     * @param hcPartyId the HCP related to the entities.
     * @param patient the Patient related to the entities.
     * @param sfks the Secret Foreign keys used to filter the entities.
     * @param excludedIds a list of IDs of entities to exclude.
     * @param includeIrrelevantInformation wether to include entities that are irrelevant.
     * @param services the Services to join in the MD5 signature. If null, they will be retrieved from the database.
     * @param healthElements the Health Elements to join in the MD4
     * @return the MD5 signature
     */
    suspend fun getMd5(
        hcPartyId: String,
        patient: Patient,
        sfks: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        services: List<Service>? = null,
        healthElements: List<HealthElement>? = null,
    ): String {
        val hcPartyIds = healthcarePartyLogic.getHealthcareParty(hcPartyId)?.let { healthcarePartyLogic.getHcpHierarchyIds(it) }

        val (treatedServiceIds, latestHealthElements) = hcPartyIds?.let { hcpIds ->
            getHealthElements(hcpIds, sfks, excludedIds, includeIrrelevantInformation, healthElements)
                .let { result ->
                    Pair(
                        result.first,
                        result.second.map { it.modified.toString() }
                    )
                }
        } ?: Pair(emptySet(), emptyList())

        val signatures = hcPartyIds?.let { hcpIds ->
            getAllServices(hcpIds, sfks, excludedIds, includeIrrelevantInformation, null, services)
                .filter { !treatedServiceIds.contains(it.id) }
                .fold(latestHealthElements + patient.getSignature()) { acc, it ->
                    acc + it.modified.toString()
                }
        } ?: (latestHealthElements + patient.getSignature())

        return DigestUtils.md5Hex(signatures.sorted().joinToString(","))
    }

    /**
     * Creates a SumEHR XML message populated with all the medical data of a Patient and emits it as a flow
     * of DataBuffer.
     * @param pat The Patient.
     * @param sfks The Secret Foreign Keys of the Patient.
     * @param sender The HCP responsible for the SumEHR.
     * @param recipient The HCP to send the SumEHR to.
     * @param language The content language of the HealthElement.
     * @param comment
     * @param excludedIds The ids of the Services and HealthElements to exclude.
     * @param includeIrrelevantInformation Whether to include irrelevant or inactive Services and HealthElements.
     * @param decryptor The decryptor for Services and HealthElements.
     * @param services If not null, uses these Services instead of retrieving them from the db.
     * @param healthElements If not null, uses these HealthElements instead of retrieving them from the db.
     * @param config The Config used to create PatientTypes.
     * @return A flow of DataBuffer containing the SumEHR XML.
     */
    fun createSumehr(
        pat: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        recipient: HealthcareParty?,
        language: String,
        comment: String?,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
        healthElements: List<HealthElement>?,
        config: Config,
    ) = flow {
        config.defaultLanguage = when(sender.languages.firstOrNull()) {
            "nl" -> "nl-BE"
            "de" -> "de-BE"
            else -> "fr-BE"
        }
        val message = initializeMessage(sender, config)
        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(
                    recipient?.let { createParty(it, emptyList()) } ?: createParty(
                        emptyList(),
                        listOf(CDHCPARTY().apply { s = CDHCPARTYschemes.CD_APPLICATION; sv = "1.0" }),
                        "sumehr",
                    )
                )
            }
        )

        val folder = FolderType()
        folder.ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = 1.toString() })
        folder.patient = makePerson(pat, config)
        fillPatientFolder(
            folder,
            pat,
            sfks,
            sender,
            language,
            config,
            comment,
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
            healthElements,
        )
        emitMessage(message.apply { folders.add(folder) }).collect { emit(it) }
    }

    /**
     * Populates the SumEHR folder with the health information of a Patient. It adds Services, DMG manager doctor, the
     * contact people, the related HealthcareParties, vaccines, and medications.
     * @param folder The FolderType to populate.
     * @param p The Patient.
     * @param sfks The Secret Foreign Keys of the Patient.
     * @param sender The HCP responsible for the SumEHR.
     * @param language The content language of the HealthElement.
     * @param config The Config used to create PatientTypes.
     * @param comment
     * @param excludedIds The ids of the Services and HealthElements to exclude.
     * @param includeIrrelevantInformation Whether to include irrelevant or inactive Services and HealthElements.
     * @param decryptor The decryptor for Services and HealthElements.
     * @param services If not null, uses these Services instead of retrieving them from the db.
     * @param healthElements If not null, uses these HealthElements instead of retrieving them from the db.
     * @return A populated FolderType
     */
    private suspend fun fillPatientFolder(
        folder: FolderType,
        p: Patient,
        sfks: List<String>,
        sender: HealthcareParty,
        language: String,
        config: Config,
        comment: String?,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
        healthElements: List<HealthElement>?,
    ): FolderType {
        val hcpartyIds = healthcarePartyLogic.getHcpHierarchyIds(sender)
        val treatedServiceIds = mutableSetOf<String>()
        // Create transaction
        val trn = TransactionType().apply {
            cds.add(CDTRANSACTION().apply { s = CDTRANSACTIONschemes.CD_TRANSACTION; sv = "1.0"; value = "sumehr" })
            author = AuthorType().apply { hcparties.add(createParty(sender, emptyList())) }
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = "1" })
            ids.add(
                IDKMEHR().apply {
                    s = IDKMEHRschemes.LOCAL; sl = "iCure-Item"; sv = kmehrConfig.kmehrVersion; value =
                        p.id.replace("-".toRegex(), "").substring(0, 8) + "." + System.currentTimeMillis()
                },
            )
            Utils.makeXGC(System.currentTimeMillis()).let { date = it; time = it }
            isIscomplete = true
            isIsvalidated = true
        }

        folder.transactions.add(trn)

        // healthelement healthissue
        treatedServiceIds.addAll(
            addHealthCareElements(
                hcpartyIds,
                sfks,
                trn,
                excludedIds,
                includeIrrelevantInformation,
                decryptor,
                healthElements,
            ),
        )

        // risks
        addActiveServiceUsingContent(
            hcpartyIds,
            sfks,
            trn,
            "adr",
            language,
            excludedIds,
            treatedServiceIds,
            decryptor,
            services,
            includeIrrelevantInformation = includeIrrelevantInformation,
        )
        addActiveServiceUsingContent(
            hcpartyIds,
            sfks,
            trn,
            "allergy",
            language,
            excludedIds,
            treatedServiceIds,
            decryptor,
            services,
            includeIrrelevantInformation = includeIrrelevantInformation,
        )
        addActiveServiceUsingContent(
            hcpartyIds,
            sfks,
            trn,
            "socialrisk",
            language,
            excludedIds,
            treatedServiceIds,
            decryptor,
            services,
            includeIrrelevantInformation = includeIrrelevantInformation,
        )
        addActiveServiceUsingContent(
            hcpartyIds,
            sfks,
            trn,
            "risk",
            language,
            excludedIds,
            treatedServiceIds,
            decryptor,
            services,
            includeIrrelevantInformation = includeIrrelevantInformation,
        )

        // people: gmdmanager/contact/patienthcp
        addGmdmanager(p, trn)
        addContactPeople(p, trn, config, excludedIds)
        addPatientHealthcareParties(p, trn, excludedIds)

        // patientwill (not included: omissionofmedicaldata --> this is added automatically)
        addActiveServicesAsCD(
            hcpartyIds,
            sfks,
            trn,
            "patientwill",
            CDCONTENTschemes.CD_PATIENTWILL,
            listOf(
                "bloodtransfusionrefusal", "clinicaltrialparticipationconsent", "datareuseforclinicalresearchconsent",
                "datareuseforclinicaltrialsconsent", "euthanasiarequest", "intubationrefusal",
                "organdonationconsent", "vaccinationrefusal", "ntbr",
            ),
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
            language
        )

        addActiveServicesAsCDPatientWillChoice(
            hcpartyIds,
            sfks,
            trn,
            "patientwill",
            CDCONTENTschemes.CD_PATIENTWILL_HOS,
            listOf(
                "hospitalisation",
            ),
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
            language
        )

        addActiveServicesAsCDPatientWillChoice(
            hcpartyIds,
            sfks,
            trn,
            "patientwill",
            CDCONTENTschemes.CD_PATIENTWILL_RES,
            listOf(
                "resuscitation",
            ),
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
            language
        )

        // vac/med
        addVaccines(
            hcpartyIds,
            sfks,
            trn,
            excludedIds,
            decryptor,
            services,
            language
        )
        addMedications(
            hcpartyIds,
            sfks,
            trn,
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
            language
        )

        addActiveServiceUsingContent(
            hcpartyIds,
            sfks,
            trn,
            "healthissue",
            language,
            excludedIds,
            treatedServiceIds,
            decryptor,
            services,
            false,
            "problem",
            includeIrrelevantInformation,
        )
        addActiveServiceUsingContent(
            hcpartyIds,
            sfks,
            trn,
            "healthcareelement",
            language,
            excludedIds,
            treatedServiceIds,
            decryptor,
            services,
            false,
            "problem",
            includeIrrelevantInformation,
        )

        addNoContentItemIfNeeded(trn, "problem")
        addNoContentItemIfNeeded(trn, "treatment")

        // global comment
        if ((comment?.length ?: 0) > 0) {
            trn.headingsAndItemsAndTexts.add(
                TextType().apply {
                    l = sender.languages.firstOrNull() ?: "fr"; value = comment
                },
            )
        }
        // Remove empty headings
        val iterator = folder.transactions[0].headingsAndItemsAndTexts.iterator()
        while (iterator.hasNext()) {
            val h = iterator.next()
            if (h is HeadingType) {
                if (h.headingsAndItemsAndTexts.size == 0) {
                    iterator.remove()
                }
            }
        }
        return folder
    }

    /**
     * If the TransactionType passed as parameter has no CD-ITEM of the type passed as parameter, it creates on ItemType
     * with that type and no content and adds it to the transaction.
     * @param trn the TransactionType.
     * @param type the item type.
     */
    private fun addNoContentItemIfNeeded(trn: TransactionType, type: String) {
        val assessmentItems = getAssessment(trn).headingsAndItemsAndTexts
        val hasItem =
            (assessmentItems + getHistory(trn).headingsAndItemsAndTexts).filterIsInstance(ItemType::class.java)
                .any { item ->
                    item.cds.filterNotNull().any { cd ->
                        cd.s == CDITEMschemes.CD_ITEM && cd.value == type
                    }
                }
        if (!hasItem) {
            assessmentItems.add(
                ItemType().apply {
                    ids.add(
                        IDKMEHR().apply {
                            s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = (assessmentItems.size + 1).toString()
                        },
                    )
                    cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); nullFlavor = "NA"; value = type })
                    lifecycle = LifecycleType().apply {
                        cd = CDLIFECYCLE().apply {
                            s = "CD-LIFECYCLE"
                            value = CDLIFECYCLEvalues.INACTIVE
                        }
                    }
                },
            )
        }
    }

    /**
     * Gets all the active Services, the one related to Medications and Vaccines for a list of HCPs and Patients.
     * @param hcPartyIds the list of ids of HCP.
     * @param sfks the list of Patient Secret Foreign Keys.
     * @param excludedIds the list of ids of Services to exclude.
     * @param includeIrrelevantInformation whether to include Services that are inactive or irrelevant.
     * @param decryptor the decryptor for the Services.
     * @param services if not null, these services are used instead of retrieving them from the db.
     * @return a list of Services.
     */
    suspend fun getAllServices(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt? = null,
        services: List<Service>? = null,
    ): List<Service> =
        getActiveServices(
            hcPartyIds,
            sfks,
            listOf("adr", "allergy", "socialrisk", "risk", "patientwill", "healthissue", "healthcareelement"),
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
        ) + getMedications(
            hcPartyIds,
            sfks,
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            services,
        ) + getVaccines(
            hcPartyIds,
            sfks,
            excludedIds,
            decryptor,
            services,
        ).filter { s -> !excludedIds.contains(s.id) }

    private fun isInactiveAndIrrelevant(it: HealthElement) =
        ServiceStatus.isIrrelevant(it.status) && (it.closingDate != null || ServiceStatus.isInactive(it.status))

    private fun isInactiveAndIrrelevant(s: Service) =
        (ServiceStatus.isInactive(s.status) || s.tags.any { it.type == "CD-LIFECYCLE" && it.code == "inactive" }) && // Inactive
            ServiceStatus.isIrrelevant(s.status)

    /**
     * If the list of services passed as parameter is not null, this function will return all the ones whose tags have
     * at least one code with the type "CD-ITEM" and code equal to one of the codes passed as parameter.
     * Otherwise, it retrieves all the codes for this HCP, with the patient secret foreign keys provided and with at
     * least one tag with the type "CD-ITEM" and code equal to one of the codes passed as parameter. Then, it filters
     * out the ones that are inactive, are irrelevant (until explicitly specified) or have no content. It also filters
     * out the one specified by the user.
     * Then, if a decryptor is passed, decrypts the ones with an encrypted self not empty.
     *
     * @param hcPartyIds the IDs of the HCP related to the Services.
     * @param sfks the Secret Foreign Keys of the patients related to the services.
     * @param cdItems the codes of the tag to be included.
     * @param excludedIds the ids of the Services to filter out.
     * @param includeIrrelevantInformation whether to include inactive and irrelevant Services or not
     * @param decryptor a decryptor for the Services
     * @param services a list of Service to pass if they are not to be retrieved from the database
     * @return a list of Services
     */
    private suspend fun getActiveServices(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        cdItems: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
    ): List<Service> {
        if (services != null) {
            return services.filter { s -> s.tags.any { c -> cdItems.contains(c.code) && c.type == "CD-ITEM" } }
        }

        val serviceIds = hcPartyIds.flatMap { hcpId ->
            cdItems.flatMap { tagCode ->
                contactLogic.listServiceIdsByTag(hcpId, sfks, "CD-ITEM", tagCode, null, null).toList()
            }
        }

        val filteredServices = contactLogic.getServices(serviceIds)
            .toList()
            .filter { s ->
                s.endOfLife == null && // Not end of lifed
                    (if (includeIrrelevantInformation) !isInactiveAndIrrelevant(s) else !ServiceStatus.isIrrelevant(s.status)) &&
                    (
                        s.content.values.any {
                            null != (it.binaryValue ?: it.booleanValue ?: it.documentId ?: it.instantValue ?: it.measureValue
                            ?: it.medicationValue) || (it.stringValue?.length ?: 0) > 0
                        } || (s.encryptedContent?.length ?: 0) > 0 || (s.encryptedSelf?.length ?: 0) > 0) // And content
            }.filter { s -> !excludedIds.contains(s.id) }

        val servicesToBeDecrypted = filteredServices.filter {
            (it.encryptedContent?.length ?: 0) > 0 || (it.encryptedSelf?.length ?: 0) > 0
        }

        return if (decryptor != null && servicesToBeDecrypted.isNotEmpty()) {
                val decryptedServices = servicesToBeDecrypted.chunked(50)
                    .fold(emptyList<Service>()) { acc, itt ->
                        acc + decryptor.decrypt(itt.map { serviceMapper.map(it) }, ServiceDto::class.java).map { serviceMapper.map(it) }
                    }
                filteredServices.map {
                    if (servicesToBeDecrypted.contains(it))
                        decryptedServices[servicesToBeDecrypted.indexOf(it)]
                    else it
                }
            } else {
                filteredServices
            }.distinctBy { s -> s.contactId + s.id }
    }

    /**
     * Given a list of iCureDocuments, returns  all the ones that are not confidential and not visible according to
     * their tags and codes.
     * @param items the list of items to filter
     * @return the filtered list
     */
    private fun <T : ICureDocument<String>> getNonConfidentialItems(items: List<T>): List<T> =
        items.filter { s ->
            null == s.tags.find { it.type == "org.taktik.icure.entities.embed.Confidentiality" && it.code == "secret" } &&
                null == s.codes.find { it.type == "org.taktik.icure.entities.embed.Visibility" && it.code == "maskedfromsummary" }
        }

    /**
     * Checks if the current transaction has already an item of which content is a will of omission of medical data.
     * @param trn the TransactionType.
     * @return true if the content exists, false otherwise.
     */
    private fun hasOmissionOfMedicalDataItem(trn: TransactionType): Boolean =
        getAssessment(trn).headingsAndItemsAndTexts
            .filter { it != null && it is ItemType }
            .map { it as ItemType }
            .any { item ->
                item.contents.filterNotNull().any { content ->
                    content.cds.filterNotNull().any { cd ->
                        cd.value == CDPATIENTWILLvalues.OMISSIONOFMEDICALDATA.value()
                    }
                }
            }


    /**
     * Returns the list of HealthElements provided by the user, if not null, otherwise retrieves them based on the HCPs
     * and Patient SFKs passed as parameters. It filters out the irrelevant HealthElements, the excluded HealthElements,
     * and the ones that have a familyrisk code.
     * @param hcPartyIds the ids of the HCPs related to the HealthElements to retrieve.
     * @param sfks the Secret Foreign Keys of the Patients related to the HealthElements to retrieve.
     * @param excludedIds the id of the HealthElements to exclude.
     * @param includeIrrelevantInformation whether to include HealthElements with irrelevant information.
     * @param healthElements if it is not null, is used instead of retrieving the HealthElements from the db
     * @return a Pair where the first element is the set of Service ids corresponding to the HealthElements and the second is the list of HealthElements
     */
    suspend fun getHealthElements(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        healthElements: List<HealthElement>? = null,
    ): Pair<Set<String>, List<HealthElement>> =
        (healthElements ?:
            hcPartyIds.flatMap { healthElementLogic.listLatestHealthElementsByHcPartyAndSecretPatientKeys(it, sfks,) }
        ).asSequence()
            .let { he ->
                Pair(
                    he.mapNotNull { it.idService }.toSet(),
                    he.filter {
                        !(it.descr?.matches("INBOX|Etat g\\u00e9n\\u00e9ral.*".toRegex()) ?: false) &&
                                (if (includeIrrelevantInformation) !isInactiveAndIrrelevant(it) else !ServiceStatus.isIrrelevant(it.status))
                    }.filter {
                            s -> !excludedIds.contains(s.id)
                    }.filter {
                            s -> !s.tags.any { t -> t.code == "familyrisk" }
                    }.distinctBy {
                            s -> s.healthElementId
                    }.toList()
                )
            }

    /**
     * Add, once and only once an will of omission of medical data to the current transaction.
     * @param trn the TransactionType.
     */
    private fun addOmissionOfMedicalDataItem(trn: TransactionType) {
        if (!hasOmissionOfMedicalDataItem(trn)) {
            val assessment = getAssessment(trn)
            assessment.headingsAndItemsAndTexts.add(super.getOmissionOfMedicalDataWill(assessment.headingsAndItemsAndTexts.size + 1))
        }
    }

    /**
     * Checks if a predicate is verified on two lists of ICureDocuments: a confidentials one and a non-confidential one.
     * If the predicate is true, adds a will of omission of medical data to the current transaction.
     * @param trn the TransactionType
     * @param items the first list of Documents
     * @param nonConfidentialItems the second list of Documents
     * @param predicate
     */
    private fun <T : ICureDocument<String>> addOmissionOfMedicalDataItem(
        trn: TransactionType,
        items: List<T>,
        nonConfidentialItems: List<T>,
        predicate: ((List<T>, List<T>) -> Boolean) = { a, b -> a.size != b.size },
    ) {
        if (predicate(items, nonConfidentialItems)) {
            addOmissionOfMedicalDataItem(trn)
        }
    }

    /**
     * Gets all the partnerships from a Patient but the ones to exclude.
     * @param excludedIds The ids of the partnerships to exclude.
     * @param patientId The id of the Patient
     * @return The list of Partnership related to the Patient
     */
    suspend fun getContactPeople(
        excludedIds: List<String>,
        patientId: String,
    ): List<Partnership> =
        patientLogic.getPatient(patientId)?.partnerships?.filter { p -> !excludedIds.contains(p.partnerId) }
            ?: emptyList()

    suspend fun getPatientHealthCareParties(
        excludedIds: List<String>,
        patientId: String
    ): List<PatientHealthCareParty> =
        patientLogic.getPatient(patientId)?.patientHealthCareParties?.filter { p -> !excludedIds.contains(p.healthcarePartyId) }
            ?: emptyList()

    /**
     * Gets all the services related to medications or treatment that are active and not closed.
     * @param hcPartyIds the HCP IDs related to the Services.
     * @param sfks the Secret Foreign Keys of the patients related to the Services.
     * @param excludedIds the IDs of the Services to exclude.
     * @param includeIrrelevantInformation whether to include Services that are irrelevant.
     * @param decryptor a decryptor for the Services.
     * @param services a list of Services to pass not to retrieve them from the database.
     * @return a list of Services fulfilling the criteria.
     */
    private suspend fun getMedications(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
    ): List<Service> {
        val now = LocalDateTime.now()

        // Chronic medications
        val medications = getActiveServices(
            hcPartyIds,
            sfks,
            listOf("medication"),
            emptyList(),
            includeIrrelevantInformation,
            decryptor,
            services,
        ).filter {
            getMedicationServiceClosingDate(it)?.let { date -> FuzzyValues.getDateTime(date)?.isAfter(now) != false } ?: true
        }

        val drugCnkCodes = medications
            .filter { m -> m.codes.find { it.type == "CD-DRUG-CNK" } != null }
            .mapNotNull { m -> m.codes.find { it.type == "CD-DRUG-CNK" }?.code }
            .toMutableSet()

        // Prescriptions
        return medications.filter { !excludedIds.contains(it.id) } + getActiveServices(
            hcPartyIds,
            sfks,
            listOf("treatment"),
            excludedIds,
            false,
            decryptor,
            services,
        ).filter {
            val cnk = it.codes.find { code -> code.type == "CD-DRUG-CNK" }?.code
            val res = (null == cnk || !drugCnkCodes.contains(cnk)) && (
                (null == getMedicationServiceClosingDate(it) && FuzzyValues.compare(
                    (it.openingDate ?: it.valueDate ?: 1970101),
                    FuzzyValues.getFuzzyDate(LocalDateTime.now().minusWeeks(2), ChronoUnit.SECONDS)) > 0) ||
                (getMedicationServiceClosingDate(it)?.let { date ->
                        FuzzyValues.getDateTime(date)?.isAfter(now) != false
                    } ?: false
                )
            )
            cnk?.let { code -> drugCnkCodes.add(code) }
            res
        }
    }

    /**
     * Gets the closing value of a service from the closing date or from the first medicationValue that has a non-null
     * end moment.
     * @param service the Service to get the closing date from
     * @return the closing date as timestamp or null if no one is found
     */
    private fun getMedicationServiceClosingDate(service: Service): Long? =
        service.closingDate ?: service.content.values.firstNotNullOfOrNull {
            it.medicationValue?.endMoment?.let { endMoment ->
                FuzzyValues.getFuzzyDateTime(
                    FuzzyValues.getDateTime(endMoment) ?: throw IllegalArgumentException("Wrong date: $endMoment"),
                    ChronoUnit.SECONDS,
                )
            }
        }

    /**
     * Gets all the active Services associated to vaccines. Always excludes ones that are irrelevant.
     * @param hcPartyIds the HCP IDs related to the Services.
     * @param sfks the Secret Foreign Keys of the patients related to the Services.
     * @param excludedIds the IDs of the Services to exclude.
     * @param decryptor a decryptor for the Services.
     * @param services a list of Services to pass not to retrieve them from the database.
     * @return a list of Services fulfilling the criteria.
     */
    private suspend fun getVaccines(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        excludedIds: List<String>,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
    ): List<Service> =
        getActiveServices(
            hcPartyIds,
            sfks,
            listOf("vaccine"),
            excludedIds,
            false,
            decryptor,
            services,
        ).filter { it.codes.any { c -> c.type == "CD-VACCINEINDICATION" && (c.code?.length ?: 0) > 0 } }

    /**
     * Extract the assessment HeadingType from a TransactionType oe creates a new one.
     * @param trn the TransactionType
     * @return the assessment HeadingType
     */
    private fun getAssessment(trn: TransactionType): HeadingType {
        val assessment = trn.headingsAndItemsAndTexts.find { h -> (h is HeadingType) && h.cds.any { cd -> cd.value == "assessment" } }
            ?: HeadingType().apply {
                ids.add(
                    IDKMEHR().apply {
                        s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = (trn.headingsAndItemsAndTexts.size + 1).toString()
                    },
                )
                cds.add(CDHEADING().apply { s = CDHEADINGschemes.CD_HEADING; sv = "1.0"; value = "assessment" })
            }.also { trn.headingsAndItemsAndTexts.add(it) }
        return assessment as HeadingType
    }

    /**
     * Extract the history HeadingType from a TransactionType ore creates a new one
     * @param trn the TransactionType
     * @return the history HeadingType
     */
    private fun getHistory(trn: TransactionType): HeadingType {
        val history = trn.headingsAndItemsAndTexts.find { h -> (h is HeadingType) && h.cds.any { cd -> cd.value == "history" } }
            ?:HeadingType().apply {
                ids.add(
                    IDKMEHR().apply {
                        s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = (trn.headingsAndItemsAndTexts.size + 1).toString()
                    },
                )
                cds.add(CDHEADING().apply { s = CDHEADINGschemes.CD_HEADING; sv = "1.0"; value = "history" })
            }.also { trn.headingsAndItemsAndTexts.add(it) }
        return history as HeadingType
    }

    /**
     * Retrieves the Services related to a set of HCPs and Patients (through their SFKs), gets the non-confidential
     * ones and adds them to the current transaction as CD items.
     * @param hcPartyIds The ids of the HCPs related to the Services.
     * @param sfks The Secret Foreign Keys of the Patients related to the Services.
     * @param trn The TransactionType.
     * @param cdItem
     * @param type
     * @param values
     * @param excludedIds The ids of the Services to exclude.
     * @param includeIrrelevantInformation Whether to include Services that are inactive or irrelevant.
     * @param decryptor The decryptor for the services.
     * @param servicesFromClient If it is not null, uses this services instead of retrieving them.
     * @param language The language to set for the items.
     */
    private suspend fun addActiveServicesAsCD(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        trn: TransactionType,
        cdItem: String,
        type: CDCONTENTschemes,
        values: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        servicesFromClient: List<Service>?,
        language: String
    ) {
        val items = trn.headingsAndItemsAndTexts

        val services = getActiveServices(
            hcPartyIds,
            sfks,
            listOf(cdItem),
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            servicesFromClient,
        )
        val nonConfidentialItems = getNonConfidentialItems(services)
        addOmissionOfMedicalDataItem(trn, services, nonConfidentialItems)

        values.forEach { value ->
            nonConfidentialItems.filter { s -> null != s.codes.find { it.type == type.value() && value == it.code } }.forEach { service ->
                createItemWithContent(
                    service,
                    items.filter{ it !is HeadingType }.size + 1,
                    cdItem,
                    listOf(ContentType().apply { cds.add(CDCONTENT().apply { s = type; sv = "1.3"; this.value = value }) }),
                    language = language
                )?.let {
                        items.add(it)
                }
            }
        }
    }

    /**
     * Gets all the active services related to a set of HCP and a Patient, through its Secret Foreign Keys, or uses the
     * ones passed as parameter.
     * Then, for all the ones that are not confidential and have one of the codes passed as parameter, creates a new
     * KmEHR ItemType and adds it to the TransactionType passed as parameter.
     * @param hcPartyIds The ids of the HCP related to the Services.
     * @param sfks The Secret Foreign Keys of the Patient related to the Services.
     * @param trn The TransactionType.
     * @param cdItem The CD-ITEM of the Services to retrieve.
     * @param itemCodes Only the Services that contain one of this codes will be added.
     * @param excludedIds The ids of the Services to exclude
     * @param includeIrrelevantInformation Whether to include Services that are irrelevant or expired.
     * @param decryptor The decryptor for the Services.
     * @param servicesFromClient If not null, these Services are used instead of retrieving them from the db.
     * @param language The language of the ItemTypes that will be created.
     */
    private suspend fun addActiveServicesAsCDPatientWillChoice(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        trn: TransactionType,
        cdItem: String,
        type: CDCONTENTschemes,
        itemCodes: List<String>,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        servicesFromClient: List<Service>?,
        language: String,
    ) {
        val items = trn.headingsAndItemsAndTexts

        val services = getActiveServices(
            hcPartyIds,
            sfks,
            listOf(cdItem),
            excludedIds,
            includeIrrelevantInformation,
            decryptor,
            servicesFromClient,
        )
        val nonConfidentialItems = getNonConfidentialItems(services)
        addOmissionOfMedicalDataItem(trn, services, nonConfidentialItems)

        itemCodes.forEach { value ->
            nonConfidentialItems.filter { s -> null != s.codes.find { it.type == type.value() && value == it.code } }
                .forEach { service ->
                    val contentCode = service.content.firstNotNullOfOrNull { (_, value) ->
                        value.stringValue
                    } ?: ""
                    createItemWithContent(
                        service,
                        items.filter{ it !is HeadingType }.size + 1,
                        cdItem, listOf(ContentType().apply {
                            cds.add(CDCONTENT().apply { s = type; sv = "1.3"; this.value = contentCode })
                        }),
                        language = language
                    )?.let {
                        items.add(it)
                    }
            }
        }
    }

    /**
     * Retrieves the active services for a set of HCPs and Patients (through its Secret Foreign Keys), it creates a
     * KmEHR ItemType for each of them that is not confidential and adds them to the current transaction.
     * @param hcPartyIds the ids of the HCPs involved in the SumEHR.
     * @param sfks The Secret Foreign Keys of the patients.
     * @param trn The TransactionType.
     * @param cdItem The CD-ITEM code related to the services to include.
     * @param language The language of the medication content to include.
     * @param excludedIds The ids of the Services to exclude
     * @param treatedServiceIds The ids of the Service that have already been treated
     * @param decryptor The decryptor for the Services.
     * @param servicesFromClient If not null, will use this Services instead of retrieving them.
     * @param forcePassive
     * @param forceCdItem The CD-ITEM to force for all Services.
     * @param includeIrrelevantInformation whether to include the services that are irrelevant.
     */
    private suspend fun addActiveServiceUsingContent(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        trn: TransactionType,
        cdItem: String,
        language: String,
        excludedIds: List<String>,
        treatedServiceIds: Set<String>,
        decryptor: AsyncDecrypt?,
        servicesFromClient: List<Service>?,
        forcePassive: Boolean = false,
        forceCdItem: String? = null,
        includeIrrelevantInformation: Boolean = false,
    ) {
        try {
            val services = getActiveServices(
                hcPartyIds,
                sfks,
                listOf(cdItem),
                excludedIds,
                includeIrrelevantInformation,
                decryptor,
                servicesFromClient,
            )
            val nonConfidentialItems = getNonConfidentialItems(services).also {
                addOmissionOfMedicalDataItem(trn, services, it)
            }.filter { s -> !treatedServiceIds.contains(s.id) }

            if (nonConfidentialItems.isEmpty()) {
                log.debug("_writeItems : no services found with cd-item $cdItem")
            } else {
                nonConfidentialItems.forEach { svc ->
                    val items =
                        if (!((svc.tags.any { it.type == "CD-LIFECYCLE" && it.code == "inactive" } || ServiceStatus.isInactive(svc.status)) && !forcePassive)) {
                            getAssessment(trn).headingsAndItemsAndTexts
                        } else {
                            getHistory(trn).headingsAndItemsAndTexts
                        }

                    val it = createItemWithContent(
                        svc,
                        items.size + 1,
                        forceCdItem ?: cdItem,
                        (
                            svc.content[language]?.let { makeContent(language, it) } ?: svc.content.entries.firstOrNull()
                                ?.let { makeContent(it.key, it.value) }
                            )?.let { listOf(it) } ?: emptyList(),
                        language = language,
                    )
                    if (it != null) {
                        it.contents.addAll(
                            listOf(
                                ContentType().apply {
                                    svc.codes.forEach { c ->
                                        try {
                                            // CD-ATC have a version 0.0.1 in the DB. However the sumehr validator requires a CD-ATC 1.0
                                            val version = if (c.type == "CD-ATC") "1.0" else c.version
                                            // BE-THESAURUS (IBUI) are in fact CD-CLINICAL (https://www.ehealth.fgov.be/standards/kmehr/en/tables/ibui)
                                            val type = if (c.type == "BE-THESAURUS") "CD-CLINICAL" else c.type
                                            val cdt = CDCONTENTschemes.fromValue(type)
                                            this.cds.add(
                                                CDCONTENT().apply {
                                                    s(cdt); sl = type; dn = type; sv = version; value = c.code
                                                },
                                            )
                                        } catch (ignored: IllegalArgumentException) {
                                            log.error(ignored)
                                        }
                                    }
                                },
                            ).filter { (it.cds?.size ?: 0) > 0 },
                        )
                        svc.content.entries
                            .firstOrNull { (_, value) -> value.medicationValue != null }
                            ?.let { (key, _) ->
                                fillMedicationItem(svc, it, key)
                            }


                        if (svc.comment != null) {
                            it.texts.add(TextType().apply { l = "fr"; value = svc.comment })
                        }
                        if ((it.contents?.size ?: 0) > 0) {
                            items.add(it)
                        }
                    }
                }
            }
        } catch (e: RuntimeException) {
            log.error("Unexpected error", e)
        }
    }

    /**
     * Creates a KmEHR ItemType containing the data related to the vaccine Service passed as parameter.
     * @param svc the iCure Service related to the vaccine.
     * @param itemIndex the ItemTYpe index.
     * @return The ItemType or null.
     */
    private suspend fun createVaccineItem(svc: Service, itemIndex: Int, language: String): ItemType? =
       createItemWithContent(
            svc,
            itemIndex,
            "vaccine",
            listOf(
                svc.content.entries.firstNotNullOf {
                    makeContent(
                        it.key,
                        it.value.copy(
                            booleanValue = null,
                            binaryValue = null,
                            documentId = null,
                            measureValue = null,
                            numberValue = null,
                            instantValue = null,
                            stringValue = null,
                        ),
                    )
                },
            ),
            language = language,
        )?.also {
            // item.contents = item.contents.distinctBy{it -> it.medicinalproduct.intendedname}
            addServiceCodesAndTags(
                svc,
                it,
                true,
                listOf("CD-ATC", "CD-VACCINEINDICATION"),
                null,
                listOf("CD-TRANSACTION", "CD-TRANSACTION-TYPE"),
            )
        }

    override fun createItemWithContent(
        svc: Service,
        idx: Int,
        cdItem: String,
        contents: List<ContentType>,
        localIdName: String,
        language: String,
        texts: List<TextType>?,
        link: LnkType?,
        config: Config,
        altBeginMoment: Long?,
        altEndMoment: Long?,
    ): ItemType? =
        if (ServiceStatus.isAbsent(svc.status) || svc.tags.any { t -> t.type == "CD-LIFECYCLE" && t.code == "notpresent" }) {
            null
        } else super.createItemWithContent(
            svc,
            idx,
            cdItem,
            contents,
            localIdName,
            language,
            texts,
            link,
            config,
            altBeginMoment,
            altEndMoment,
        )

    override fun createItemWithContent(
        he: HealthElement,
        idx: Int,
        cdItem: String,
        contents: List<ContentType>,
    ): ItemType? =
        if (ServiceStatus.isAbsent(he.status) || he.tags.any { t -> t.type == "CD-LIFECYCLE" && t.code == "notpresent" }) {
            null
        } else super.createItemWithContent(he, idx, cdItem, contents)

    /**
     * Gets all the Partnership patients from the current patient and adds them to the current transaction.
     * @param pat the Patient where to extract the relationships.
     * @param trn the TransactionType.
     * @param config the configuration used to create a PersonType
     * @param excludedIds the ids of the Partners to exclude
     */
    private suspend fun addContactPeople(
        pat: Patient,
        trn: TransactionType,
        config: Config,
        excludedIds: List<String>,
    ) {
        pat.partnerships.filter { s -> !excludedIds.contains(s.partnerId) }.mapNotNull { it.partnerId }.let {
            patientLogic.getPatients(it).toList().forEach { p ->
                val rel = pat.partnerships.find { partner -> partner.partnerId == p.id }?.type.toString()
                try {
                    rel.let {
                        val items = trn.headingsAndItemsAndTexts
                        items.add(
                            ItemType().apply {
                                ids.add(IDKMEHR().apply {
                                    s = IDKMEHRschemes.ID_KMEHR
                                    sv = "1.0"
                                    value = (items.filter{ item ->  item !is HeadingType }.size + 1).toString() })
                                cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = CDITEMvalues.CONTACTPERSON.value() })
                                cds.add(CDITEM().apply { s(CDITEMschemes.CD_CONTACT_PERSON); value = rel })
                                contents.add(ContentType().apply { person = makePerson(p, config, true) })
                            },
                        )
                    }
                } catch (e: RuntimeException) {
                    log.error("Unexpected error", e)
                }
            }
        }
    }

    /**
     * Gets all the Healthcare Parties related to a Patients and adds them to the current transaction as a HcpartyType.
     * @param pat the Patient
     * @param trn the TransactionType
     * @param excludedIds the ids of the HealthcareParties to exclude
     */
    private suspend fun addPatientHealthcareParties(
        pat: Patient,
        trn: TransactionType,
        excludedIds: List<String>
    ) {
        pat.patientHealthCareParties.filter { s -> !excludedIds.contains(s.healthcarePartyId) }.mapNotNull { it.healthcarePartyId }.let {
            healthcarePartyLogic.getHealthcareParties(it).toList().forEach { hcp ->
                if (hcp.specialityCodes.none { c -> c.code?.startsWith("pers") == false }) {
                    val phcp = pat.patientHealthCareParties.find { phcp -> phcp.healthcarePartyId == hcp.id }
                    try {
                        phcp.let {
                            val items = trn.headingsAndItemsAndTexts
                            items.add(
                                ItemType().apply {
                                    ids.add(IDKMEHR().apply {
                                        s = IDKMEHRschemes.ID_KMEHR
                                        sv = "1.0"
                                        value = (items.filter { item -> item !is HeadingType }.size + 1).toString()
                                    })
                                    cds.add(CDITEM().apply {
                                        s(CDITEMschemes.CD_ITEM); value = CDITEMvalues.CONTACTHCPARTY.value()
                                    })
                                    contents.add(ContentType().apply { hcparty = createParty(hcp, emptyList()) })
                                }
                            )
                        }
                    } catch (e: RuntimeException) {
                        log.error("Unexpected error", e)
                    }
                }
            }
        }
    }

    /**
     * Added the HCP responsible for the GMD (Dossier Medical Global) for the patient passed as parameter to the current
     * transaction.
     * @param pat the Patient
     * @param trn the TransactionType
     */
    private suspend fun addGmdmanager(pat: Patient, trn: TransactionType) {
        try {
            val gmdRelationship =
                pat.patientHealthCareParties.find { it.referralPeriods.any { r -> r.startDate?.isBefore(Instant.now()) == true && null == r.endDate } }
            if (gmdRelationship != null) {
                gmdRelationship.healthcarePartyId?.let {
                    healthcarePartyLogic.getHealthcareParty(it)?.let { hcp ->
                        val items = getAssessment(trn).headingsAndItemsAndTexts
                        items.add(
                            ItemType().apply {
                                ids.add(IDKMEHR().apply {
                                    s = IDKMEHRschemes.ID_KMEHR
                                    sv = "1.0"
                                    value = (items.filter{ item -> item !is HeadingType}.size + 1).toString()
                                })
                                cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = "gmdmanager" })
                                contents.add(ContentType().apply { hcparty = createParty(hcp, emptyList()) })
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Unexpected error", e)
        }
    }

    /**
     * Gets all the Services related to medications of a set of Patients which a set of HCP can access and adds them to
     * the current transaction as KmEHR ItemTypes.
     * @param hcPartyIds The ids of the HCPs related to the Services.
     * @param sfks The Secret Foreign Keys of the Patients related to the services.
     * @param trn The TransactionType.
     * @param excludedIds The ids of the Services to exclude.
     * @param includeIrrelevantInformation Whether include Services that are irrelevant or inactive.
     * @param decryptor The decryptor for the services.
     * @param services If it is not null, it uses this Services instead of retrieving them.
     * @param language The language to set in the ItemTypes.
     */
    private suspend fun addMedications(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        trn: TransactionType,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
        language: String
    ) {
        try {
            val medications =
                getMedications(hcPartyIds, sfks, excludedIds, includeIrrelevantInformation, decryptor, services)
            val nonConfidentialItems = getNonConfidentialItems(medications)
            addOmissionOfMedicalDataItem(trn, medications, nonConfidentialItems)

            nonConfidentialItems.forEach { m ->
                val items = trn.headingsAndItemsAndTexts
                createItemWithContent(
                    m.copy(closingDate = m.closingDate ?: FuzzyValues.getFuzzyDate(LocalDateTime.now().plusMonths(1), ChronoUnit.SECONDS)),
                    items.filter{ it !is HeadingType }.size + 1,
                    "medication",
                    m.content.entries.mapNotNull {
                        makeContent(
                            it.key,
                            (if ((it.value.booleanValue == true || it.value.instantValue != null || it.value.numberValue != null) && (it.value.stringValue?.length ?: 0) == 0)
                                it.value.copy(stringValue = m.label)
                            else it.value).copy(
                                booleanValue = null,
                                binaryValue = null,
                                documentId = null,
                                measureValue = null,
                                numberValue = null,
                                instantValue = null,
                            )
                        )
                    },
                    language = language,
                )?.let { item ->
                    if ((item.contents?.size ?: 0) > 0) {
                        val medicationEntry = m.content.entries.find { null != it.value.medicationValue }
                        if (medicationEntry != null) {
                            fillMedicationItem(m, item, medicationEntry.key)
                        }
                        items.add(item)
                    }
                }
            }
        } catch (e: RuntimeException) {
            log.error("Unexpected error", e)
        }
    }

    /**
     * Gets all the Services related to vaccines of a set of Patients which a set of HCP can access and adds them to the
     * current transaction as KmEHR ItemTypes.
     * @param hcPartyIds The ids of the HCPs related to the Services.
     * @param sfks The Secret Foreign Keys of the Patients related to the services.
     * @param trn The TransactionType.
     * @param excludedIds The ids of the Services to exclude.
     * @param decryptor The decryptor for the services.
     * @param services If it is not null, it uses this Services instead of retrieving them.
     * @param language The language to set in the ItemTypes.
     */
    private suspend fun addVaccines(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        trn: TransactionType,
        excludedIds: List<String>,
        decryptor: AsyncDecrypt?,
        services: List<Service>?,
        language: String
    ) {
        try {
            val vaccines = getVaccines(hcPartyIds, sfks, excludedIds, decryptor, services)
            val nonConfidentialItems = getNonConfidentialItems(vaccines)
            addOmissionOfMedicalDataItem(trn, vaccines, nonConfidentialItems)

            nonConfidentialItems.forEach {
                val items = trn.headingsAndItemsAndTexts
                items.add(createVaccineItem(it, items.filter{ item -> item !is HeadingType }.size + 1, language))
            }
        } catch (e: RuntimeException) {
            log.error("Unexpected error", e)
        }
    }

    /**
     * Retrieves a set of HealthElements (or uses the ones passed as parameter), gets the non-confidential one and
     * includes them in the current transaction.
     * @param hcPartyIds the ids of the HCP related to the HealthElements to retrieve
     * @param sfks the Secret Foreign Keys of the Patient relate to the HealthElements to retrieve
     * @param trn the TransactionType
     * @param excludedIds the ids of the HealthElements to exclude
     * @param includeIrrelevantInformation whether to include irrelevant HealthElements
     * @param decryptor the Decryptor for the HealthElement. Only decrypted HealthElement can be added to the transaction.
     * @param hesFromClient if not null, these HealthElements will be used instead of retrieving them
     * @return the set of Services related to the retrieved Health Elements
     */
    private suspend fun addHealthCareElements(
        hcPartyIds: Set<String>,
        sfks: List<String>,
        trn: TransactionType,
        excludedIds: List<String>,
        includeIrrelevantInformation: Boolean,
        decryptor: AsyncDecrypt?,
        hesFromClient: List<HealthElement>?,
    ): Set<String> {
        val retrievedHesWithServices = getHealthElements(hcPartyIds, sfks, excludedIds, includeIrrelevantInformation)
        val healthElements = hesFromClient ?: retrievedHesWithServices.second

        val nonConfidentialItems = getNonConfidentialItems(healthElements)
        addOmissionOfMedicalDataItem(trn, healthElements, nonConfidentialItems)


        // Decrypt everything so that the frontend has a chance to fix the tags
        if (decryptor != null && nonConfidentialItems.isNotEmpty()) {
            decryptor.decrypt(
                nonConfidentialItems.map { healthElementMapper.map(it) },
                HealthElementDto::class.java,
            ).map {
                healthElementMapper.map(it)
            }.forEach {
                addHealthCareElement(trn, it)
            }
        }

        return retrievedHesWithServices.first
    }

    /**
     * Adds a HealthElement to the current transaction after converting it into a SumEHR-supported format.
     * @param trn the TransactionType
     * @param he the HealthElement
     */
    private suspend fun addHealthCareElement(trn: TransactionType, he: HealthElement) {
        try {
            val items = if (he.closingDate != null && he.closingDate != 0L && he.closingDate!! < LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).toLong()) {
                getHistory(trn).headingsAndItemsAndTexts
            } else {
                getAssessment(trn).headingsAndItemsAndTexts
            }

            val sanitisedTags = he.tags.map {
                when {
                    (it.type == "CD-ITEM" && it.code == "familyrisk") -> it.copy(code = "problem", version = "1.11")
                    (it.type == "CD-ITEM" && it.code == "healthcareelement") -> it.copy(code = "problem", version = "1.11")
                    (it.type == "CD-ITEM" && it.code == "healthissue") -> it.copy(code = "problem", version = "1.11")
                    (it.type == "CD-ITEM" && it.code == "surgery") -> it.copy(code = "treatment", version = "1.11")
                    else -> it
                }
            }

            listOf("problem", "allergy", "adr", "risk", "socialrisk", "treatment").forEach { edType ->
                if (sanitisedTags.find { it.type == "CD-ITEM" && it.code == edType } != null) {
                    createItemWithContent(
                        he.copy(tags = sanitisedTags.toSet()),
                        items.size + 1,
                        edType,
                        listOfNotNull(makeContent("fr", Content(he.descr))),
                    )?.let {
                        he.note?.trim()?.let { note ->
                            if (note.isNotEmpty()) {
                                it.texts.add(
                                    TextType().apply {
                                        value = note; l = "fr"
                                    },
                                )
                            }
                        }
                        if (he.codes.isNotEmpty()) {
                            // Notice the content can not be empty (sumehr validator)
                            it.contents.addAll(
                                listOf(
                                    ContentType().apply {
                                        he.codes.forEach { c ->
                                            try {
                                                val type = if (c.type == "BE-THESAURUS") "CD-CLINICAL" else c.type
                                                val cdt = CDCONTENTschemes.fromValue(type)
                                                // CD-ATC have a version 0.0.1 in the DB. However, the sumehr validator requires a CD-ATC 1.0
                                                val version = if (c.type == "CD-ATC") "1.0" else c.version
                                                // BE-THESAURUS (IBUI) are in fact CD-CLINICAL (https://www.ehealth.fgov.be/standards/kmehr/en/tables/ibui)
                                                if (c.type != "CD-HCPARTY") {
                                                    this.cds.add(
                                                        CDCONTENT().apply {
                                                            s(cdt); sl = type; dn = type; sv = version; value = c.code
                                                        },
                                                    )
                                                }
                                            } catch (ignored: IllegalArgumentException) {
                                                log.warn("Code has been ignored for CD-CONTENT", ignored)
                                            }
                                        }
                                    },
                                ).filter { content ->  (content.cds?.size ?: 0) > 0 },
                            )
                        }
                        if ((it.contents?.size ?: 0) > 0) {
                            items.add(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Unexpected error", e)
        }
    }

    override fun addServiceCodesAndTags(
        svc: Service,
        item: ItemType,
        skipCdItem: Boolean,
        restrictedTypes: List<String>?,
        uniqueTypes: List<String>?,
        excludedTypes: List<String>?,
    ) {
        super.addServiceCodesAndTags(
            svc,
            item,
            skipCdItem,
            restrictedTypes,
            uniqueTypes,
            (excludedTypes ?: emptyList()) + listOf(
                "LOCAL",
                "RELEVANCE",
                "SUMEHR",
                "SOAP",
                "CD-TRANSACTION",
                "CD-TRANSACTION-TYPE",
            ),
        )
    }
}
