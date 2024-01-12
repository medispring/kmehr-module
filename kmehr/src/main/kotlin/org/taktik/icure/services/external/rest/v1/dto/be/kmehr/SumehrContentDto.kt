/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.be.kmehr

import org.taktik.icure.services.external.rest.v1.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PartnershipDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PatientHealthCarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import java.io.Serializable

class SumehrContentDto : Serializable {
    var services: List<ServiceDto>? = null
    var healthElements: List<HealthElementDto>? = null
    var partnerships: List<PartnershipDto>? = null
    var patientHealthcareParties: List<PatientHealthCarePartyDto>? = null
}
