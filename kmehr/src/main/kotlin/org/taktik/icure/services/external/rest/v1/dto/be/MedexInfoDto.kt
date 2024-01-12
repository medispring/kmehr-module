package org.taktik.icure.services.external.rest.v1.dto.be

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.PatientDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedexInfoDto(
    val beginDate: Long,
    val endDate: Long,
    val author: HealthcarePartyDto? = null,
    val patient: PatientDto? = null,
    val patientLanguage: String = "fr",
    val incapacityType: String = "incapacity", // incapacity or incapacityextension

    /*
        Possible values:
        illness
        hospitalisation
        sickness
        pregnancy
        workaccident
        occupationaldisease
     */
    val incapacityReason: String = "sickness",
    val outOfHomeAllowed: Boolean = true,
    /*
    "Optional field
    But mandatory when incapacityreason = workaccident; this field must contain the accident date.
    when incapacityreason = occupationaldisease this field must contain the request date for a dossier for occupatialdesease.
    This date must be < or =  beginmoment of the incapacity period."
     */
    val certificateDate: Long? = null,
    val contentDate: Long? = null,
    val diagnosisICPC: String? = null,
    val diagnosisICD: String? = null,
    val diagnosisDescr: String? = null
)
