/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.samv2.stub.PharmaceuticalFormStub

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AmpComponent(
	override val from: Long? = null,
	override val to: Long? = null,
	val ingredients: Set<Ingredient>? = null,
	val pharmaceuticalForms: Set<PharmaceuticalFormStub>? = null,
	val routeOfAdministrations: Set<RouteOfAdministration>? = null,
	val dividable: String? = null,
	val scored: String? = null,
	val crushable: Crushable? = null,
	val containsAlcohol: ContainsAlcohol? = null,
	val sugarFree: Boolean? = null,
	val modifiedReleaseType: Int? = null,
	val specificDrugDevice: Int? = null,
	val dimensions: String? = null,
	val name: SamText? = null,
	val note: SamText? = null
) : DataPeriod
