/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.v20161201

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.apache.commons.logging.LogFactory
import org.taktik.commons.uti.UTI
import org.taktik.icure.asynclogic.CodeLogic
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.Utils
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDADDRESS
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDADDRESSschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDADMINISTRATIONUNIT
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCERTAINTY
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCERTAINTYvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENT
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCOUNTRY
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDCOUNTRYschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDDRUGCNK
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDDRUGCNKschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDDRUGROUTE
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTY
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTYschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDINNCLUSTER
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDINNCLUSTERschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDITEM
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDLIFECYCLE
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDLIFECYCLEvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDLNKvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDMEDIATYPEvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDMESSAGE
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDMESSAGEvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDPATIENTWILLvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDSEX
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDSEXvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDSTANDARD
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTELECOM
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTELECOMschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTEMPORALITY
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTEMPORALITYvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTIMEUNIT
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTIMEUNITschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDUNIT
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDUNITschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDWEEKDAY
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.CDWEEKDAYvalues
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.cd.v1.LnkType
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.dt.v1.TextType
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTY
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTYschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHR
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDKMEHRschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENT
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENTschemes
import org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1.*
import org.taktik.icure.be.ehealth.logic.getAndDecryptMainAttachment
import org.taktik.icure.be.ehealth.logic.kmehr.Config
import org.taktik.icure.config.KmehrConfiguration
import org.taktik.icure.constants.ServiceStatus
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.PlanOfAction
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.FuzzyValues
import java.io.OutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.swing.text.rtf.RTFEditorKit
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import org.taktik.icure.entities.embed.AddressType as iCureAddressType

open class KmehrExport (
    val codeLogic: CodeLogic,
    val documentLogic: DocumentLogic,
    val kmehrConfig: KmehrConfiguration
) {
    private val unitCodes = mutableMapOf<String, Code>()

    val standard = "20161201"

    internal open val log = LogFactory.getLog(KmehrExport::class.java)

    /**
     * Creates a KmEHR HcpartyType adding all the IDHCPARTY and CDHCPARTY passed as parameters.
     * @param ids a list of IDHCPARTY
     * @param cds a list fo CDHCPARTY
     * @param name the name
     * @return a HcpartyType
     */
    fun createParty(ids: List<IDHCPARTY>, cds: List<CDHCPARTY>, name: String): HcpartyType {
        return HcpartyType().apply { this.ids.addAll(ids); this.cds.addAll(cds); this.name = name }
    }

    /**
     * Converts a HealthcareParty entity to a HcpartyType to be converted to a XML supported by KmEHR.
     * @param m the iCure HealthcareParty.
     * @param cds the codes to add to the HcpartyType cds.
     * @return a HcpartyType.
     */
    suspend fun createParty(m: HealthcareParty, cds: List<CDHCPARTY>? = listOf()): HcpartyType =
        HcpartyType().apply {
            if (!m.nihii.isNullOrBlank()) {
                m.nihii.let { nihii -> ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value = nihii!!.replace("[^0-9]".toRegex(), "") }) }
            }
            if (!m.ssin.isNullOrBlank()) {
                m.ssin.let { ssin -> ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.INSS; sv = "1.0"; value = ssin!!.replace("[^0-9]".toRegex(), "") }) }
            }
            cds?.let { this.cds.addAll(it) }
            this.cds.addAll(
                listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = "persphysician" }),
            )

            if (this.cds.filter { it.s == CDHCPARTYschemes.CD_HCPARTY }.any { it.value.startsWith("pers") }) {
                firstname = m.firstName
                familyname = m.lastName
            } else {
                name = m.name?.trim().let {
                    when (it) {
                        null, "" -> listOfNotNull(m.firstName, m.lastName).joinToString(" ").trim()
                        else -> it
                    }
                }
            }
            val addressesExcludingHome = m.addresses.filter { it.addressType != iCureAddressType.home }
            addresses.addAll(makeAddresses(addressesExcludingHome))
            telecoms.addAll(makeTelecoms(addressesExcludingHome))
        }


    /**
     * Creates a HcpartyType with a HUB CD_HCPARTY code.
     * @param m the iCure HCP.
     * @return a HcpartyType.
     */
    fun createHubHcParty(m: HealthcareParty): HcpartyType =
        HcpartyType().apply {
            if (!m.nihii.isNullOrBlank()) {
                m.nihii.let { nihii -> ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.ID_HCPARTY; sv = "1.0"; value = nihii!!.replace("[^0-9]".toRegex(), "") }) }
            }
            this.cds.addAll(
                listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = "hub" }),
            )
            name = m.name?.trim()
        }

    /**
     * Creates a KmEHR HcPartyType from an iCure HCP. If the HCP has no speciality codes, "persphysician" is used.
     * @param m The iCure HCP.
     * @param cds the cds to add to the HcpartyType.
     * @return The HcpartyType.
     */
    fun createSpecialistParty(m: HealthcareParty, cds: List<CDHCPARTY>? = listOf()): HcpartyType =
        HcpartyType().apply {
            cds?.let { this.cds.addAll(it) }
            this.cds.addAll(
                if (m.specialityCodes.isNotEmpty()) {
                    m.specialityCodes.map { CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = it.code } }
                } else if ((m.speciality ?: "") != "") {
                    listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = m.speciality })
                } else {
                    listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = "persphysician" })
                },
            )
        }

    /**
     * Creates a new PersonType from an iCurePatient using the config passed as parameter.
     * @param p the iCure Patient.
     * @param config the Config.
     * @return a PersonType
     */
    suspend fun makePatient(p: Patient, config: Config): PersonType {
        val ssin = p.ssin?.replace("[^0-9]".toRegex(), "")?.takeIf { org.taktik.icure.utils.Math.isNissValid(it) }
        return makePerson(p, config).apply {
            ids.clear()
            ssin?.let { ssin -> ids.add(IDPATIENT().apply { s = IDPATIENTschemes.ID_PATIENT; sv = "1.0"; value = ssin.replace("[^0-9]".toRegex(), "") }) }
            ids.add(IDPATIENT().apply { s = IDPATIENTschemes.LOCAL; sl = "MF-ID"; sv = "1.0"; value = p.id })
        }
    }

    /**
     * Converts an iCurePatient to a KmEHR PersonType.
     * @param p the iCure Patient
     * @param config options to create the PersonType
     * @param useINSSId if true, uses the INSS as Patient id.
     * @return a PersonType instance
     */
    suspend fun makePerson(p: Patient, config: Config, useINSSId: Boolean? = false): PersonType =
        makePersonBase(p, config, useINSSId).apply {
            p.dateOfDeath?.let { if (it != 0) deathdate = Utils.makeDateTypeFromFuzzyLong(it.toLong()) }
            p.placeOfBirth?.let { birthlocation = AddressTypeBase().apply { city = it } }
            p.placeOfDeath?.let { deathlocation = AddressTypeBase().apply { city = it } }
            p.profession?.let { profession = ProfessionType().apply { text = TextType().apply { l = "fr"; value = it } } }
            usuallanguage = if (config.format == Config.Format.SUMEHR || config.format == Config.Format.MEDICATIONSCHEME) config.defaultLanguage else p.languages.firstOrNull()
            addresses.addAll(makeAddresses(p.addresses))
            telecoms.addAll(makeTelecoms(p.addresses))
            if (!p.nationality.isNullOrBlank()) {
                p.nationality.let { nat -> nationality = PersonType.Nationality().apply { cd = CDCOUNTRY().apply { s(CDCOUNTRYschemes.CD_COUNTRY); value = nat } } }
            }
        }

    /**
     * Converts an iCure Patient to a KmEHR PersonType containing only the base information (ssin, patient id, names,
     * sex, date of birth).
     * @param p the iCure Patient.
     * @param config options to create the PersonType.
     * @param useINSSId if true, uses the INSS as Patient id.
     * @return a PersonType instance
     */
    private fun makePersonBase(p: Patient, config: Config, useINSSId: Boolean? = false): PersonType {
        val ssin = p.ssin?.replace("[^0-9]".toRegex(), "")
            ?.takeIf { org.taktik.icure.utils.Math.isNissValid(it) }

        return PersonType().apply {
            ssin?.let { ssin ->
                if (useINSSId == true)
                    ids.add(IDPATIENT().apply { s = IDPATIENTschemes.INSS; sv = "1.0"; value = ssin.replace("[^0-9]".toRegex(), "") })
                else ids.add(IDPATIENT().apply { s = IDPATIENTschemes.ID_PATIENT; sv = "1.0"; value = ssin.replace("[^0-9]".toRegex(), "") })
            }
            //config.soft?.version --> "1.0" to be compatible with hospital software
            p.id.let { id -> ids.add(IDPATIENT().apply { s = IDPATIENTschemes.LOCAL; sv = "1.0"; sl = "${config.soft?.name}-Person-Id"; value = id }) }
            firstnames.add(p.firstName)
            familyname = p.lastName
            sex = SexType().apply { cd = CDSEX().apply { s = "CD-SEX"; sv = "1.0"; value = p.gender?.let { CDSEXvalues.fromValue(it.name) } ?: CDSEXvalues.UNKNOWN } }
            p.dateOfBirth?.let { birthdate = Utils.makeDateTypeFromFuzzyLong(it.toLong()) }
            recorddatetime = Utils.makeXGC(p.modified, false, config.format == Config.Format.MEDICATIONSCHEME)
        }
    }

    /**
     * Creates a KmEHR ItemType and fills it with the information from a iCure Service.
     * @param svc the iCure Service
     * @param idx the ItemType id
     * @param cdItem
     * @param contents the Contents to add to the ItemType
     * @param localIdName
     * @param language
     * @param texts the Texts to add to the ItemType
     * @param link the Links to add to the ItemType
     * @param config
     * @param altBeginMoment
     * @param altEndMoment
     * @return an ItemType
     */
    open fun createItemWithContent(
        svc: Service,
        idx: Int,
        cdItem: String,
        contents: List<ContentType>,
        localIdName: String = "iCure-Service",
        language: String,
        texts: List<TextType>? = null,
        link: LnkType? = null,
        config: Config = Config(),
        altBeginMoment: Long? = null,
        altEndMoment: Long? = null): ItemType? =
    ItemType().apply {
        ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = idx.toString() })
        ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = localIdName; sv = kmehrConfig.kmehrVersion; value = svc.id })
        cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = cdItem })
        svc.tags.find { t -> t.type == "CD-LAB" }?.let { cds.add(CDITEM().apply { s(CDITEMschemes.CD_LAB); value = it.code }) }
        val suspension = svc.content.entries.firstNotNullOfOrNull { it.value.medicationValue }?.suspension?.firstOrNull()
        this.contents.addAll(filterEmptyContent(contents))
        if (texts != null) this.texts.addAll(texts)
        if (link != null) this.lnks.add(link)
        lifecycle = LifecycleType().apply {
            cd = CDLIFECYCLE().apply {
                s = "CD-LIFECYCLE"
					value = if ((((FuzzyValues.getDateTime(svc.closingDate?.takeIf { it > 0 } ?: 99991231)
                            ?: LocalDateTime.MAX) < LocalDateTime.now()))) {
                    CDLIFECYCLEvalues.INACTIVE
                } else {
                    svc.tags.find { t -> t.type == "CD-LIFECYCLE" }?.let {
                        try {
                            CDLIFECYCLEvalues.fromValue(it.code)
                        } catch (e: java.lang.IllegalArgumentException) {
                            CDLIFECYCLEvalues.CORRECTED
                        }
                    } ?: if (cdItem == "medication") {
                            if (suspension != null) CDLIFECYCLEvalues.fromValue(suspension.lifecycle) else CDLIFECYCLEvalues.PRESCRIBED
                        } else CDLIFECYCLEvalues.ACTIVE
                }
            }
        }
        if (cdItem == "medication") {
            svc.tags.find { it.type == "CD-TEMPORALITY" && it.code != null }?.let {
                temporality = TemporalityType().apply {
                    cd = CDTEMPORALITY().apply { s = "CD-TEMPORALITY"; value = CDTEMPORALITYvalues.fromValue(it.code!!.lowercase()) }
                }
            }
            svc.content.entries.firstNotNullOfOrNull { it.value.medicationValue }?.let { med ->
                KmehrPrescriptionHelper.inferPeriodFromRegimen(med.regimen, med.frequency)?.let {
                    frequency = KmehrPrescriptionHelper.mapPeriodToFrequency(it)
                }
                duration = KmehrPrescriptionHelper.toDurationType(med.duration.takeIf { config.format != Config.Format.MEDICATIONSCHEME })
                med.regimen?.let { intakes ->
                    if (intakes.isNotEmpty()) {
                        regimen = ItemType.Regimen().apply {
                            for (intake in intakes) {
                                // choice day specification
                                intake.dayNumber?.let { dayNumber -> daynumbersAndQuantitiesAndDates.add(BigInteger.valueOf(dayNumber.toLong())) }
                                    ?: daynumbersAndQuantitiesAndDates.add(BigInteger.valueOf(1.toLong()))
                                intake.date?.let { d -> daynumbersAndQuantitiesAndDates.add(Utils.makeXMLGregorianCalendarFromFuzzyLong(d)) }
                                intake.weekday?.let { day ->
                                    daynumbersAndQuantitiesAndDates.add(
                                        ItemType.Regimen.Weekday().apply {
                                            day.weekday?.let { dayOfWeek ->
                                                cd = CDWEEKDAY().apply { s = "CD-WEEKDAY"; value = CDWEEKDAYvalues.fromValue(dayOfWeek.code) }
                                            }
                                            day.weekNumber?.let { n -> weeknumber = BigInteger.valueOf(n.toLong()) }
                                        },
                                    )
                                }
                                // choice time of day
                                try {
                                    daynumbersAndQuantitiesAndDates.add(KmehrPrescriptionHelper.toDaytime(intake))
                                } catch (e: Exception) {
                                    log.warn("Cannot export value $intake to kmehr in regimen")
                                }

                                // mandatory quantity
                                intake.administratedQuantity?.let { drugQuantity ->
                                    daynumbersAndQuantitiesAndDates.add(
                                        AdministrationquantityType().apply {
                                            decimal = drugQuantity.quantity?.let { BigDecimal(it) }
                                            drugQuantity.administrationUnit?.let { drugUnit ->
                                                unit = AdministrationunitType().apply {
                                                    cd = CDADMINISTRATIONUNIT().apply {
                                                        s = "CD-ADMINISTRATIONUNIT"
                                                        sv = "1.2"
                                                        value = drugUnit.code
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                if (this.regimen == null) {
                    (med.posology ?: med.posologyText)?.let {
                        this.posology = ItemType.Posology().apply { text = TextType().apply { l = language; value = it } }
                    }
                }
                (med.instructionForPatient)?.let {
                    this.instructionforpatient = TextType().apply { l = language; value = it }
                }
                med.renewal?.let {
                    renewal = RenewalType().apply {
                        it.decimal?.let { decimal = BigDecimal(it.toLong()) }
                        duration = KmehrPrescriptionHelper.toDurationType(it.duration)
                    }
                }
                med.drugRoute?.let { c ->
                    route = RouteType().apply { cd = CDDRUGROUTE().apply { s = "CD-DRUG-ROUTE"; value = c } }
                }
                med.temporality?.let { c ->
                    temporality = TemporalityType().apply {
                        cd = CDTEMPORALITY().apply {
                            s = "CD-TEMPORALITY"
                            value = CDTEMPORALITYvalues.fromValue(c)
                        }
                    }
                }
            }
        }

        isIsrelevant = ServiceStatus.isRelevant(svc.status)
        beginmoment = (altBeginMoment ?: svc.content.entries.firstNotNullOfOrNull { it.value.medicationValue }?.beginMoment ?: svc.valueDate ?: svc.openingDate)
            ?.let { if (it != 0L) Utils.makeMomentTypeDateFromFuzzyLong(it) else null }
        endmoment = (altEndMoment ?: svc.closingDate ?: svc.content.entries.firstNotNullOfOrNull { it.value.medicationValue }?.endMoment)
            ?.let { if (it != 0L) Utils.makeMomentTypeDateFromFuzzyLong(it) else null }
        recorddatetime = Utils.makeXGC(
            svc.modified
                ?: svc.created,
            false,
            config.format == Config.Format.MEDICATIONSCHEME,
        )
    }

    /**
     * Creates an ItemType of will of omission of medical data.
     * @param idx the ItemType id
     * @return the ItemType
     */
    open fun getOmissionOfMedicalDataWill(idx: Int): ItemType =
        ItemType().apply {
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = idx.toString() })
            cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = CDITEMvalues.PATIENTWILL.value() })
            contents.add(ContentType().apply {
                cds.add(CDCONTENT().apply {
                    s(CDCONTENTschemes.CD_PATIENTWILL)
                    value = CDPATIENTWILLvalues.OMISSIONOFMEDICALDATA.value()
                })
            })
            lifecycle = LifecycleType().apply { cd = CDLIFECYCLE().apply { s = "CD-LIFECYCLE"; value = CDLIFECYCLEvalues.ACTIVE } }
            isIsrelevant = true
            beginmoment = Utils.makeMomentTypeDateFromFuzzyLong(FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.DAYS))
            endmoment = Utils.makeMomentTypeDateFromFuzzyLong(FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.DAYS))
            recorddatetime = Utils.makeXGC(Instant.now().toEpochMilli())
        }

    private fun filterEmptyContent(contents: List<ContentType>) = contents.filter {
        it.isBoolean != null || (it.cds?.size ?: 0) > 0 || it.bacteriology != null || it.compoundprescription != null ||
            it.location != null || (it.lnks?.size ?: 0) > 0 || it.bacteriology != null || it.ecg != null || it.holter != null ||
            it.medication != null || it.compoundprescription != null || it.substanceproduct != null || it.medicinalproduct != null ||
            it.error != null || it.incapacity != null || it.insurance != null || it.person != null || it.hcparty != null ||
            it.date != null || it.time != null || it.yearmonth != null || it.year != null || (it.texts?.size ?: 0) > 0 ||
            it.unsignedInt != null || it.decimal != null || (it.cds?.size ?: 0) > 0 || (it.ids?.size ?: 0) > 0 ||
            it.unit != null || it.minref != null || it.maxref != null || (it.refscopes?.size ?: 0) > 0
    }

    /**
     * Creates a KmEHR ItemType with a Content from an iCure HealthElement.
     * @param he the iCure Service.
     * @param idx the KmEHR id of the ItemType.
     * @param cdItem the CD-ITEM scheme code.
     * @param contents the list of contents to add to the ItemType.
     * @return the ItemType.
     */
    open fun createItemWithContent(he: HealthElement, idx: Int, cdItem: String, contents: List<ContentType>): ItemType? =
        ItemType().apply {
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = idx.toString() })
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "iCure-HealthElement"; sv = kmehrConfig.kmehrVersion; value = he.id })
            cds.add(CDITEM().apply { s(CDITEMschemes.CD_ITEM); value = cdItem })

            this.contents.addAll(filterEmptyContent(contents))
            lifecycle = LifecycleType().apply {
                cd = CDLIFECYCLE().apply {
                    s = "CD-LIFECYCLE"
                    value = if (!ServiceStatus.isActive(he.status) || ( (he.closingDate ?: 0) > FuzzyValues.getCurrentFuzzyDateTime(ChronoUnit.DAYS))) {
                        CDLIFECYCLEvalues.INACTIVE
                    } else {
                        he.tags.find { t -> t.type == "CD-LIFECYCLE" }?.let { CDLIFECYCLEvalues.fromValue(it.code) } ?: CDLIFECYCLEvalues.ACTIVE
                    }
                }
            }

            certainty = he.tags.find { t -> t.type == "CD-CERTAINTY" && !t.code.isNullOrBlank() }?.let {
                CertaintyType().apply {
                    cd = CDCERTAINTY().apply {
                        s = "CD-CERTAINTY"; value = try {
                            CDCERTAINTYvalues.fromValue(it.code)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
            isIsrelevant = ServiceStatus.isRelevant(he.status)
            beginmoment = (he.openingDate ?: he.valueDate).let { if (it != 0L) Utils.makeMomentTypeFromFuzzyLong(it) else null }
            endmoment = he.closingDate?.let { if (it != 0L) Utils.makeMomentTypeFromFuzzyLong(it) else null }
            recorddatetime = Utils.makeXGC(he.modified)
        }

    /**
     * Extracts the Telecoms from a collection of iCure Addresses and adds them to a List of TelecomTypes to be serialized
     * in a XML supported by KmEHR.
     * @param addresses the Collection of addresses
     * @return a list of TelecomType
     */
    private fun makeTelecoms(addresses: Collection<Address>?): List<TelecomType> =
        addresses?.filter { it.addressType != null }?.flatMapTo(ArrayList()) { a ->
            a.telecoms.filter { (it.telecomNumber?.length ?: 0) > 0 }.map {
                TelecomType().apply {
                    cds.add(CDTELECOM().apply { s(CDTELECOMschemes.CD_ADDRESS); value = a.addressType?.name ?: "home" })
                    cds.add(CDTELECOM().apply { s(CDTELECOMschemes.CD_TELECOM); value = it.telecomType?.name ?: "phone" })
                    telecomnumber = it.telecomNumber
                }
            }
        } ?: emptyList()

    fun makeDummyTelecom(): List<TelecomType> {
        val lst = ArrayList<TelecomType>()
        lst.add(
            TelecomType().apply {
                cds.add(CDTELECOM().apply { s(CDTELECOMschemes.CD_ADDRESS); value = "home" })
                cds.add(CDTELECOM().apply { s(CDTELECOMschemes.CD_TELECOM); value = "phone" })
                telecomnumber = "00.00.00"
            },
        )
        return lst
    }

    /**
     * Converts a list of Address entities from iCure to a List of AddressTypes to be serialized in a XML supported
     * by KmEHR.
     * @param addresses a Collection of iCure Address.
     * @return a List of AddressType.
     */
    private suspend fun makeAddresses(addresses: Collection<Address>?): List<AddressType> =
        addresses?.filter { it.addressType != null && it.postalCode != null && it.street != null }?.mapTo(ArrayList()) { a ->
            AddressType().apply {
                cds.add(CDADDRESS().apply { s(CDADDRESSschemes.CD_ADDRESS); value = a.addressType!!.name })
                country = if ((a.country?.length ?: 0) > 0)
                    mapToCountryCode(a.country!!)?.let { natCode ->
                        CountryType().apply {
                            cd = CDCOUNTRY().apply {
                                s(CDCOUNTRYschemes.CD_FED_COUNTRY)
                                value = natCode
                            }
                        }
                    } else CountryType().apply { cd = CDCOUNTRY().apply { s(CDCOUNTRYschemes.CD_FED_COUNTRY); value = "be" } }
                zip = a.postalCode ?: "0000"
                street = a.street ?: "unknown"
                if (!a.houseNumber.isNullOrBlank()) {
                    a.houseNumber.let { housenumber = a.houseNumber }
                }
                if (!a.postboxNumber.isNullOrBlank()) {
                    a.postboxNumber.let { postboxnumber = a.postboxNumber }
                }
                city = a.city ?: "unknown"
            }
        } ?: emptyList()


    fun makeDummyAddress(): List<AddressType> {
        val lst = ArrayList<AddressType>()
        lst.add(
            AddressType().apply {
                cds.add(CDADDRESS().apply { s(CDADDRESSschemes.CD_ADDRESS); value = "work" })
                country = CountryType().apply { cd = CDCOUNTRY().apply { s(CDCOUNTRYschemes.CD_FED_COUNTRY); value = "be" } }
                zip = "0000"
                street = "unknown"
                city = "unknown"
            },
        )
        return lst
    }

    fun makeText(language: String, content: Content): TextType? {
        return if (!content.medicationValue?.compoundPrescription.isNullOrEmpty()) {
            TextType().apply {
                l = language
                value = content.medicationValue?.medicinalProduct?.intendedname
            }
        } else if ((content.medicationValue?.endMoment ?: -1) > 0 && !content.medicationValue?.endCondition.isNullOrEmpty()) {
            TextType().apply {
                l = language
                value = content.medicationValue?.endCondition // set endreason as text of the medication item, NOTE: endreason does not exist yet on the medication object, endcondition is used instead MS-6840
            }
        } else {
            null
        }
    }

    /**
     * Creates a KmEHR ContentType from an iCureContent
     * @param language the language of the Content
     * @param content the iCure Content
     * @return a ContentType
     */
    suspend fun makeContent(language: String, content: Content): ContentType? =
        (content.booleanValue ?: content.numberValue ?: content.stringValue ?: content.instantValue
                ?: content.measureValue ?: content.medicationValue ?: content.binaryValue ?: content.documentId
            ).let {
            ContentType().apply {
                isBoolean = content.booleanValue
                content.numberValue?.let { decimal = BigDecimal.valueOf(it) }
                content.stringValue?.let {
                    if (content.binaryValue == null && content.documentId == null) {
                        texts.add(TextType().apply { l = language; value = content.stringValue })
                    }
                }
                Utils.makeXGC(content.instantValue?.toEpochMilli(), true)?.let { date = it; time = it; }
                content.measureValue?.let { mv ->
                    mv.unitCodes?.find { it.type == "CD-UNIT" }?.code?.let { unitCode ->
                        if (unitCode.isNotEmpty()) {
                            unit = UnitType().apply { cd = CDUNIT().apply { s(CDUNITschemes.CD_UNIT); value = unitCode } }
                        }
                    }
                    if (unit == null) {
                        mv.unit?.let { getCode(it)?.let { unit = UnitType().apply { cd = CDUNIT().apply { s(CDUNITschemes.CD_UNIT); value = it.code } } } }
                    }
                    mv.value?.let { decimal = BigDecimal.valueOf(it) }
                }
                content.medicationValue?.medicinalProduct?.let {
                    if (content.medicationValue!!.compoundPrescription == null) {
                        val medicationType = content.medicationValue!!.options?.get("type")?.stringValue
                        medicinalproduct = MedicinalProductType().apply {
                            intendedname = it.intendedname
                            if (medicationType != "FRE") {
                                intendedcds.add(CDDRUGCNK().apply {
                                    s(CDDRUGCNKschemes.CD_DRUG_CNK) /* TODO set versions in jaxb classes */
                                    sv = "01-2016"
                                    value = it.intendedcds.find { it.type == "CD-DRUG-CNK" }?.code
                                })
                            }
                        }
                    }
                }
                content.medicationValue?.substanceProduct?.let { substanceProduct ->
                    val innCluster = substanceProduct.intendedcds.find { it.type == "CD-INNCLUSTER" }?.code
                    val vmpGroup = substanceProduct.intendedcds.find { it.type == "CD-VMPGROUP" }?.code

                    substanceproduct = ContentType.Substanceproduct().apply {
                        intendedname = substanceProduct.intendedname
                        intendedcd =
                            if (vmpGroup.isNullOrEmpty()) {
                                CDINNCLUSTER().apply {
                                    s(CDINNCLUSTERschemes.CD_INNCLUSTER) /* TODO set versions in jaxb classes */
                                    sv = "01-2016"
                                    value = innCluster
                                }
                            } else {
                                CDINNCLUSTER().apply {
                                    s(CDINNCLUSTERschemes.CD_VMPGROUP) /* TODO set versions in jaxb classes */
                                    sv = "01-2016"
                                    value = vmpGroup
                                }
                            }
                    }
                }
                content.medicationValue?.compoundPrescription?.let {
                    medicinalproduct = null // todo check if this works (remove the if(content.medicationValue?.compoundPrescription == null) { ...)
                    compoundprescription = CompoundprescriptionType().apply {
                        l = language
                        this.content.add(ObjectFactory().createCompoundprescriptionTypeMagistraltext(TextType().apply { l = language; value = it }))
                    }
                }
                content.binaryValue?.let { byteArray ->
                    if (byteArray.slice(0..4).toByteArray().contentEquals("{\\rtf".toByteArray())) {
                        texts.add(
                            TextType().apply {
                                l = language
                                value = RTFEditorKit().let {
                                    val document = it.createDefaultDocument()
                                    it.read(byteArray.inputStream(), document, 0)
                                    document.getText(0, document.length) ?: ""
                                }
                            },
                        )
                    } else {
                        lnks.add(LnkType().apply { type = CDLNKvalues.MULTIMEDIA; mediatype = CDMEDIATYPEvalues.APPLICATION_PDF; value = content.binaryValue })
                    }
                }
                content.documentId?.let {
                    try {
                        documentLogic.getDocument(it)?.let { d -> documentLogic.getAndDecryptMainAttachment(d.id)?.let { lnks.add(LnkType().apply { type = CDLNKvalues.MULTIMEDIA; mediatype = documentMediaType(d); value = it }) } }
                    } catch (e: Exception) {
                        log.warn("Document with id $it could not be loaded", e)
                    }
                }
            }
        }.takeIf {
                it.isBoolean != null || it.date != null || it.time != null || it.lnks.size > 0 || it.compoundprescription != null
                        || it.substanceproduct != null || it.medicinalproduct != null || it.cds.size > 0 || it.decimal != null || it.texts.size > 0
        }

    protected fun documentMediaType(d: Document) =
        (listOf(d.mainUti) + d.otherUtis).map {
            UTI.get(it)?.mimeTypes?.firstOrNull()?.let {
                try {
                    CDMEDIATYPEvalues.fromValue(it)
                } catch (ignored: IllegalArgumentException) {
                    null
                }
            }
        }.filterNotNull().firstOrNull()

    /**
     * Adds the medication-related content information of a Service to a KmEHR ItemType.
     * @param svc the iCure Service
     * @param item the ItemType
     * @param lang the language related to the content to consider
     */
    fun fillMedicationItem(svc: Service, item: ItemType, lang: String) {
        addServiceCodesAndTags(svc, item, true, listOf("CD-ATC"), null, listOf("CD-TRANSACTION", "CD-TRANSACTION-TYPE"))

        val c = svc.content[lang]
            ?.takeIf { it.medicationValue?.let { v -> v.medicinalProduct ?: v.substanceProduct ?: v.compoundPrescription } != null }
            ?: svc.content.values.find { it.medicationValue?.let { v -> v.medicinalProduct ?: v.substanceProduct ?: v.compoundPrescription } != null }

        c.let { cnt ->
            item.contents.add(0, ContentType()
                .apply { texts.add(TextType()
                    .apply {
                        l = lang
                        value = cnt?.medicationValue?.medicinalProduct?.intendedname
                            ?: cnt?.medicationValue?.substanceProduct?.intendedname
                                    ?: cnt?.medicationValue?.compoundPrescription
                                    ?: cnt?.stringValue ?: ""
                    })
                })
            cnt?.medicationValue?.substanceProduct.let { _ ->
                cnt?.medicationValue?.duration?.let { d ->
                    if (d.value != null) {
                        item.duration = DurationType().apply {
                            decimal = BigDecimal.valueOf(d.value!!); unit = d.unit?.code?.let {
                                TimeunitType().apply { cd = CDTIMEUNIT().apply { s(CDTIMEUNITschemes.CD_TIMEUNIT); value = it } }
                            }
                        }
                    }
                }
            }
            cnt?.medicationValue?.posology?.let {
                item.posology = ItemType.Posology().apply { text = TextType().apply { l = lang; value = it } }
            }
            if (item.posology == null) {
                cnt?.medicationValue?.posologyText?.let {
                    item.posology = ItemType.Posology().apply { text = TextType().apply { l = lang; value = it } }
                }
            }
            cnt?.medicationValue?.instructionForPatient?.let {
                item.instructionforpatient = TextType().apply { l = lang; value = it }
            }
        }
    }

    /**
     * Updates a KmEHR item type with the codes and tags from a iCure Service
     * @param svc the iCure Service
     * @param item the ItemType to update
     * @param restrictedTypes
     * @param uniqueTypes the types that can be added only once
     * @param excludedTypes the types to exclude
     */
    open fun addServiceCodesAndTags(svc: Service, item: ItemType, skipCdItem: Boolean = true, restrictedTypes: List<String>? = null, uniqueTypes: List<String>? = null, excludedTypes: List<String>? = listOf("CD-TRANSACTION", "CD-TRANSACTION-TYPE")) {
        ContentType().apply {
            svc.codes.forEach { c ->
                try {
                    val cdt = CDCONTENTschemes.fromValue(c.type)
                    if ((restrictedTypes == null || restrictedTypes.contains(c.type)) && (excludedTypes == null || !excludedTypes.contains(c.type))) {
                        if (uniqueTypes == null || !uniqueTypes.contains(c.type) || this.cds.find { cc -> cdt == cc.s } == null) {
                            this.cds.add(CDCONTENT().apply { s(cdt); value = c.code })
                        } else if ((restrictedTypes == null || restrictedTypes.contains("LOCAL")) && (excludedTypes == null || !excludedTypes.contains("LOCAL"))) {
                            this.cds.add(CDCONTENT().apply { s(CDCONTENTschemes.LOCAL); sl = c.type; dn = c.type; value = c.code })
                        }
                    }
                } catch (ignored: IllegalArgumentException) {
                    if ((restrictedTypes == null || restrictedTypes.contains("LOCAL")) && (excludedTypes == null || !excludedTypes.contains("LOCAL"))) {
                        this.cds.add(CDCONTENT().apply { s(CDCONTENTschemes.LOCAL); sl = c.type; dn = c.type; value = c.code })
                    }
                }
            }

            svc.tags.forEach { c ->
                try {
                    val idt = CDITEMschemes.fromValue(c.type)
                    val prevIcc = item.cds.find { cc -> idt == cc.s }
                    if (prevIcc == null) {
                        item.cds.add(CDITEM().apply { s(idt); value = c.code })
                    } else if (prevIcc.value != c.code) {
                        item.cds.add(CDITEM().apply { s(CDITEMschemes.LOCAL); sl = c.type; dn = c.type; value = c.code })
                    }
                } catch (ignored: IllegalArgumentException) {
                    //noinspection GroovyUnusedCatchParameter
                    try {
                        val cdt = CDCONTENTschemes.fromValue(c.type)
                        if ((restrictedTypes == null || restrictedTypes.contains(c.type)) && (excludedTypes == null || !excludedTypes.contains(c.type))) {
                            val prevCc = this.cds.find { cc -> cdt == cc.s }
                            if (uniqueTypes == null || !uniqueTypes.contains(c.type) || prevCc == null) {
                                this.cds.add(CDCONTENT().apply { s(cdt); value = c.code })
                            } else if (prevCc.value != c.code && ((restrictedTypes == null || restrictedTypes.contains("LOCAL")) && (excludedTypes == null || !excludedTypes.contains("LOCAL")))) {
                                this.cds.add(CDCONTENT().apply { s(CDCONTENTschemes.LOCAL); sl = c.type; dn = c.type; value = c.code })
                            }
                        }
                    } catch (ignoredAsWell: IllegalArgumentException) {
                        if ((restrictedTypes == null || restrictedTypes.contains("LOCAL")) && (excludedTypes == null || !excludedTypes.contains("LOCAL"))) {
                            this.cds.add(CDCONTENT().apply { s(CDCONTENTschemes.LOCAL); sl = c.type; dn = c.type; value = c.code })
                        }
                    }
                }
            }

            svc.label?.let { lbl ->
                when {
                    svc.content.values.find { it.medicationValue != null } != null -> "$lbl{m}"
                    svc.content.values.find { it.measureValue != null } != null -> "$lbl{v}"
                    svc.content.values.find { it.stringValue != null } != null -> "$lbl{s}"
                    svc.content.values.find { it.numberValue != null } != null -> "$lbl{n}"
                    svc.content.values.find { it.instantValue != null } != null -> "$lbl{d}"
                    svc.content.values.find { it.binaryValue != null || it.documentId != null } != null -> "$lbl{x}"
                    svc.content.values.find { it.booleanValue != null } != null -> "$lbl{b}"
                    else -> lbl
                }
            }?.let { lbl ->
                item.cds.add(CDITEM().apply { s(CDITEMschemes.LOCAL); sl = "iCure-Label"; dn = "iCure service label"; value = lbl })
            }

            if (this.cds.size > 0) {
                item.contents.add(this)
            }
        }
    }

    private suspend fun createFolder(sender: HealthcareParty, patient: Patient, cdTransaction: String, transactionType: CDTRANSACTIONschemes, dem: PlanOfAction, ssc: Form, text: String?, attachmentDocumentIds: List<String>, config: Config): FolderType {
        return FolderType().apply {
            ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = 1.toString() })
            this.patient = makePerson(patient, config)
            transactions.add(
                TransactionType().apply {
                    cds.add(CDTRANSACTION().apply { s(transactionType); value = cdTransaction })
                    author = AuthorType().apply { hcparties.add(createParty(sender, emptyList())) }
                    ids.add(IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = "1" })
                    ids.add(IDKMEHR().apply { s = IDKMEHRschemes.LOCAL; sl = "iCure-Item"; sv = "1.0"; value = ssc.id })
                    recorddatetime = Utils.makeXGC(
                        ssc.created ?: (((dem.openingDate ?: dem.valueDate)?.let { FuzzyValues.getDateTime(it) }
                            ?: LocalDateTime.now()).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000))
                    isIscomplete = true
                    isIsvalidated = true

                    if ((text?.length ?: 0) > 0) headingsAndItemsAndTexts.add(TextType().apply { l = "fr"; value = text })
                    attachmentDocumentIds.forEach { id ->
                        documentLogic.getDocument(id)?.let { d ->
                            documentLogic.getAndDecryptMainAttachment(d.id).let {
                                headingsAndItemsAndTexts.add(
                                    LnkType().apply {
                                        type = CDLNKvalues.MULTIMEDIA; mediatype = documentMediaType(d); value = it
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    /**
     * Initializes a object to be converted into a KmEHR xml message.
     * @param sender the HCP responsible for creating the message
     * @param config the configuration for the message
     * @return a KmEHR message
     */
    suspend fun initializeMessage(sender: HealthcareParty, config: Config): Kmehrmessage {
        return Kmehrmessage().apply {
            header = HeaderType().apply {
                standard = StandardType().apply {
                    cd = CDSTANDARD().apply { s = "CD-STANDARD"; value = this@KmehrExport.standard }
                    val filetype = when(config.format) {
                        Config.Format.PMF -> CDMESSAGEvalues.GPPATIENTMIGRATION
                        Config.Format.SMF -> CDMESSAGEvalues.GPSOFTWAREMIGRATION
                        else -> null
                    }
                    filetype?.let {
                        specialisation = StandardType.Specialisation().apply {
                            cd = CDMESSAGE().apply { s = "CD-MESSAGE"; value = filetype }
                            version = SMF_VERSION
                        }
                    }
                }
                ids.add(IDKMEHR().apply {
                    s = IDKMEHRschemes.ID_KMEHR
                    sv = "1.0"
                    value = (sender.nihii ?: sender.id) + "." + (config._kmehrId ?: System.currentTimeMillis())
                })
                Utils.makeXGC(Instant.now().toEpochMilli()).let {
                    date = it
                    time = it
                }
                this.sender = SenderType().apply {
                    hcparties.add(createParty(sender, emptyList()))
                    hcparties.add(
                        HcpartyType().apply {
                            ids.add(IDHCPARTY().apply { s = IDHCPARTYschemes.LOCAL; sl = config.soft?.name; sv = "1.0"; value = "${config.soft?.name}-${config.soft?.version}" })
                            cds.add(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_HCPARTY); value = "application" })
                            name = config.soft?.name
                        },
                    )
                }
            }
        }
    }

    /**
     * Checks if a country code is valid, otherwise checks if a code with that label is present in the database and
     * returns that.
     * @param country the country code or country name
     * @return the country code or null
     */
    private suspend fun mapToCountryCode(country: String?): String? =
        country?.let { c ->
            c.lowercase().takeIf { codeLogic.isValid("CD-FED-COUNTRY", it, null) }
                ?: codeLogic.getCodeByLabel("be", c, "CD-FED-COUNTRY")?.code
        }

    suspend fun exportContactReportDynamic(patient: Patient, sender: HealthcareParty, recipient: Any?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        when (recipient) {
            is HealthcareParty -> {
                exportContactReport(patient, sender, recipient, dem, ssc, text, attachmentDocIds, config, stream)
            }
            null -> {
                exportContactReport(patient, sender, null, dem, ssc, text, attachmentDocIds, config, stream)
            }
            else -> {
                throw IllegalArgumentException("Recipient is not a doctor; a hospital or a generic recipient")
            }
        }
    }

    suspend fun exportContactReport(patient: Patient, sender: HealthcareParty, recipient: HealthcareParty?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        val message = initializeMessage(sender, config)

        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(recipient?.let { createParty(it, emptyList()) } ?: createParty(emptyList(), listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_APPLICATION) }), "gp-software-migration"))
            },
        )

        val folder = createFolder(sender, patient, "contactreport", CDTRANSACTIONschemes.CD_TRANSACTION, dem, ssc, text, attachmentDocIds, config)
        message.folders.add(folder)

        val jaxbMarshaller = JAXBContext.newInstance("org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1", com.sun.xml.bind.v2.ContextFactory::class.java.classLoader).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        jaxbMarshaller.marshal(message, stream)
    }

    suspend fun exportReportDynamic(patient: Patient, sender: HealthcareParty, recipient: Any?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        if (recipient is HealthcareParty) {
            exportReport(patient, sender, recipient, dem, ssc, text, attachmentDocIds, config, stream)
        } else if (recipient == null) {
            exportReport(patient, sender, null, dem, ssc, text, attachmentDocIds, config, stream)
        } else {
            throw IllegalArgumentException("Recipient is not a doctor; a hospital or a generic recipient")
        }
    }

    suspend fun exportReport(patient: Patient, sender: HealthcareParty, recipient: HealthcareParty?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        val message = initializeMessage(sender, config)

        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(recipient?.let { createParty(it, emptyList()) } ?: createParty(emptyList(), listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_APPLICATION) }), "gp-software-migration"))
            },
        )

        val folder = createFolder(sender, patient, "report", CDTRANSACTIONschemes.CD_TRANSACTION, dem, ssc, text, attachmentDocIds, config)
        message.folders.add(folder)

        val jaxbMarshaller = JAXBContext.newInstance("org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1", com.sun.xml.bind.v2.ContextFactory::class.java.classLoader).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        jaxbMarshaller.marshal(message, stream)
    }

    suspend fun exportNoteDynamic(patient: Patient, sender: HealthcareParty, recipient: Any?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        if (recipient is HealthcareParty) {
            exportNote(patient, sender, recipient, dem, ssc, text, attachmentDocIds, config, stream)
        } else if (recipient == null) {
            exportNote(patient, sender, null, dem, ssc, text, attachmentDocIds, config, stream)
        } else {
            throw IllegalArgumentException("Recipient is not a doctor; a hospital or a generic recipient")
        }
    }

    suspend fun exportNote(patient: Patient, sender: HealthcareParty, recipient: HealthcareParty?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        val message = initializeMessage(sender, config)

        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(recipient?.let { createParty(it, emptyList()) } ?: createParty(emptyList(), listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_APPLICATION) }), "gp-software-migration"))
            },
        )

        val folder = createFolder(sender, patient, "note", CDTRANSACTIONschemes.CD_TRANSACTION, dem, ssc, text, attachmentDocIds, config)
        message.folders.add(folder)

        val jaxbMarshaller = JAXBContext.newInstance("org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1", com.sun.xml.bind.v2.ContextFactory::class.java.classLoader).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        jaxbMarshaller.marshal(message, stream)
    }

    suspend fun exportPrescriptionDynamic(patient: Patient, sender: HealthcareParty, recipient: Any?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        if (recipient is HealthcareParty) {
            exportPrescription(patient, sender, recipient, dem, ssc, text, attachmentDocIds, config, stream)
        } else if (recipient == null) {
            exportPrescription(patient, sender, null, dem, ssc, text, attachmentDocIds, config, stream)
        } else {
            throw IllegalArgumentException("Recipient is not a doctor; a hospital or a generic recipient")
        }
    }

    suspend fun exportPrescription(patient: Patient, sender: HealthcareParty, recipient: HealthcareParty?, dem: PlanOfAction, ssc: Form, text: String, attachmentDocIds: List<String>, config: Config, stream: OutputStream) {
        val message = initializeMessage(sender, config)

        message.header.recipients.add(
            RecipientType().apply {
                hcparties.add(recipient?.let { createParty(it, emptyList()) } ?: createParty(emptyList(), listOf(CDHCPARTY().apply { s(CDHCPARTYschemes.CD_APPLICATION) }), "gp-software-migration"))
            },
        )

        val folder = createFolder(sender, patient, "prescription", CDTRANSACTIONschemes.CD_TRANSACTION, dem, ssc, text, attachmentDocIds, config)
        message.folders.add(folder)

        val jaxbMarshaller = JAXBContext.newInstance("org.taktik.icure.be.ehealth.dto.kmehr.v20161201.be.fgov.ehealth.standards.kmehr.schema.v1", com.sun.xml.bind.v2.ContextFactory::class.java.classLoader).createMarshaller()
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        jaxbMarshaller.marshal(message, stream)
    }

    protected fun CDITEM.s(scheme: CDITEMschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDTELECOM.s(scheme: CDTELECOMschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDADDRESS.s(scheme: CDADDRESSschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDCOUNTRY.s(scheme: CDCOUNTRYschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDCONTENT.s(scheme: CDCONTENTschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDHCPARTY.s(scheme: CDHCPARTYschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDTRANSACTION.s(scheme: CDTRANSACTIONschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDINNCLUSTER.s(scheme: CDINNCLUSTERschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDDRUGCNK.s(scheme: CDDRUGCNKschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDUNIT.s(scheme: CDUNITschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    protected fun CDTIMEUNIT.s(scheme: CDTIMEUNITschemes) {
        s = scheme
        sv = scheme.version() ?: "1.0"
    }

    fun idKmehr(index: Int) = IDKMEHR().apply { s = IDKMEHRschemes.ID_KMEHR; sv = "1.0"; value = index.toString() }

    fun localIdKmehrElement(itemIndex: Int, config: Config): IDKMEHR {
        return localIdKmehr("Element", (itemIndex + 1).toString(), config)
    }

    /**
     * Creates a local KmEHR id from an itemType, an id, and a config.
     * @param itemType the ItemType.
     * @param id the id
     * @param config the Config.
     * @return the ID-KMEHR.
     */
    fun localIdKmehr(itemType: String, id: String?, config: Config): IDKMEHR =
        IDKMEHR().apply {
            s = IDKMEHRschemes.LOCAL
            sv = "1.0" //to be compatible with hospital software ->config.soft?.version
            sl = "${config.soft?.name}-$itemType-Id"
            value = id
        }

    suspend fun getCode(key: String): Code? {
        if (unitCodes.isEmpty()) {
            codeLogic.findCodesBy("CD-UNIT", null, null)
                .onEach { unitCodes[it.id] = it }
                .collect()
        }
        return unitCodes[key]
    }

    companion object {
        const val SMF_VERSION = "2.3"
    }
}
