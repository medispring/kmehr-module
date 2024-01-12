/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.be.kmehr

import org.taktik.icure.services.external.rest.v1.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v1.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import java.io.Serializable

data class SumehrExportInfoDto(
    val secretForeignKeys: List<String> = emptyList(),
    val excludedIds: List<String> = emptyList(),
    val recipient: HealthcarePartyDto? = null,
    val softwareName: String? = null,
    val softwareVersion: String? = null,
    val comment: String = "",
    val includeIrrelevantInformation: Boolean? = null,
    val services: List<ServiceDto>? = null,
    val healthElements: List<HealthElementDto>? = null
) : Serializable
