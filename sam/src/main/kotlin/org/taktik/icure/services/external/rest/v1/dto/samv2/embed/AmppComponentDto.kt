/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AmppComponentDto(
	override val from: Long? = null,
	override val to: Long? = null,
	val contentType: ContentTypeDto? = null,
	val contentMultiplier: Int? = null,
	val packSpecification: String? = null,
	val deviceType: DeviceTypeDto? = null,
	val packagingType: PackagingTypeDto? = null
) : DataPeriodDto
