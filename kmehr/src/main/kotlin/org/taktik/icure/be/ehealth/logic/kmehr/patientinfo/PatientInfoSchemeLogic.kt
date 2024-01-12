/*
 * Copyright (C) 2018 Taktik SA
 *
 * This file is part of iCureBackend.
 *
 * iCureBackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * iCureBackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with iCureBackend.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.patientinfo

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient

/**
 * This Logic defines the method to export a PatientInfo XML message.
 */
interface PatientInfoFileLogic {

    /**
     * Creates a PatientInfo XML with the information provided.
     * @param patient the iCure Patient.
     * @param sender the iCure HCP responsible for the export.
     * @param language the language of the Content.
     * @return a Flow of DataBuffer containing the XML.
     */
    fun createExport(patient: Patient, sender: HealthcareParty, language: String): Flow<org.springframework.core.io.buffer.DataBuffer>
}
