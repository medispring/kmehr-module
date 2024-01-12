/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.medex

import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient

/**
 * This logic contains the method to export a XML message for Medex (Department for Medical Expertise) using the data
 * contained in iCure.
 */
interface MedexLogic {

    /**
     * Creates a MEDEX XML Message.
     * @param author the HCP responsible for the creation.
     * @param patient the Patient object of the message.
     * @param lang the language of the content.
     * @param incapacityType
     * @param incapacityReason
     * @param outOfHomeAllowed
     * @param certificateDate
     * @param contentDate
     * @param beginDate
     * @param endDate
     * @param diagnosisICD
     * @param diagnosisICPC
     * @param diagnosisDescr
     * @return the XML message as string.
     */
    suspend fun createMedex(
        author: HealthcareParty,
        patient: Patient,
        lang: String,
        incapacityType: String,
        incapacityReason: String,
        outOfHomeAllowed: Boolean,
        certificateDate: Long?,
        contentDate: Long?,
        beginDate: Long,
        endDate: Long,
        diagnosisICD: String?,
        diagnosisICPC: String?,
        diagnosisDescr: String?,
    ): String
}
