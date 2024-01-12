/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VmpComponentDto(
	val code: String? = null,
	val virtualForm: VirtualFormDto? = null,
	val routeOfAdministrations: List<RouteOfAdministrationDto>? = null,
	val name: SamTextDto? = null,
	val phaseNumber: Short? = null,
	val virtualIngredients: List<VirtualIngredientDto>? = null
) : Serializable
