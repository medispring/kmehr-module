/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.be.kmehr

import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import java.io.Serializable

data class MedicationSchemeExportInfoDto (
    val secretForeignKeys: List<String> = emptyList(),
    val services: List<ServiceDto> = emptyList(),
    val serviceAuthors: List<HealthcarePartyDto>? = null,
    val recipient: HealthcarePartyDto? = null,
    val comment: String? = null
) : Serializable
