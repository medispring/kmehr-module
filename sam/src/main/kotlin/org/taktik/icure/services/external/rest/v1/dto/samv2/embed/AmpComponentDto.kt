/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.samv2.stub.PharmaceuticalFormStubDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AmpComponentDto(
	override val from: Long? = null,
	override val to: Long? = null,
	val ingredients: List<IngredientDto>? = null,
	val pharmaceuticalForms: List<PharmaceuticalFormStubDto>? = null,
	val routeOfAdministrations: List<RouteOfAdministrationDto>? = null,
	val dividable: String? = null,
	val scored: String? = null,
	val crushable: CrushableDto? = null,
	val containsAlcohol: ContainsAlcoholDto? = null,
	val sugarFree: Boolean? = null,
	val modifiedReleaseType: Int? = null,
	val specificDrugDevice: Int? = null,
	val dimensions: String? = null,
	val name: SamTextDto? = null,
	val note: SamTextDto? = null
) : DataPeriodDto
