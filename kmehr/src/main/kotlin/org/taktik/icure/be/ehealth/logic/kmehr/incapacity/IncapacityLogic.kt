/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.be.ehealth.logic.kmehr.incapacity

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.taktik.icure.domain.be.kmehr.IncapacityExportInfo
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.Patient

/**
 * This interfaces defines the method to export the information related to a Patient Incapacity to an Incapacity
 * Notification supported by KmEHR.
 * The details about this XML format can be found here:
 * https://www.ehealth.fgov.be/standards/kmehr/en/transactions/incapacity-notification
 */
interface IncapacityLogic {

    /**
     * Exports an Incapacity as KmEHR XML message.
     * @param patient the iCure Patient.
     * @param sender the HCP responsible for the export.
     * @param language the language of the content.
     * @param exportInfo the IncapacityExportInfo to include in the message.
     * @param timeZone the timezone to include in the Config.
     * @return a Flow of DataBuffer containing the XML.
     */
    fun createIncapacityExport(
        patient: Patient,
        sender: HealthcareParty,
        language: String,
        exportInfo: IncapacityExportInfo,
        timeZone: String?
    ): Flow<DataBuffer>
}
