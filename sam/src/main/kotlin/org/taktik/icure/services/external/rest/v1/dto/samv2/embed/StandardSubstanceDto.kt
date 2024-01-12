/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class StandardSubstanceDto(
	val code: String? = null,
	val type: StandardSubstanceTypeDto? = null,
	val name: SamTextDto? = null,
	val definition: SamTextDto? = null,
	val url: String? = null
) : Serializable
