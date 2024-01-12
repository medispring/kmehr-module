package org.taktik.icure.be.ehealth.logic.kmehr.incapacity.impl.v20170601

import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Service

data class IncapacityDetail(
    val incapacityId: String,
    val notificationDate: Long,
    val retraction: Boolean,
    val dataset: String, // will not use for now, front-end will decide what is sent
    val transactionType: String,
    val incapacityreason: String,
    val beginmoment: Long,
    val endmoment: Long,
    val outofhomeallowed: Boolean,
    val incapWork: Boolean,
    val incapSchool: Boolean,
    val incapSwim: Boolean,
    val incapSchoolsports: Boolean,
    val incapHeavyphysicalactivity: Boolean,
    val diagnoseServices: List<Service>,
    val jobstatus: String, // values of CD-EMPLOYMENTSITUATION --> patient.profession.cd
    val job: String,
    val occupationalDiseaseDeclDate: Long,
    val accidentDate: Long,
    val expectedbirthgivingDate: Long,
    val maternityleaveBegin: Long,
    val maternityleaveEnd: Long, // will not be used (yet)
    val hospitalisationBegin: Long,
    val hospitalisationEnd: Long,
    val hospital: HealthcareParty?,
    val contactPersonTel: String,
    val recoveryAddress: Address?,
    val foreignStayBegin: Long,
    val foreignStayEnd: Long,
)
