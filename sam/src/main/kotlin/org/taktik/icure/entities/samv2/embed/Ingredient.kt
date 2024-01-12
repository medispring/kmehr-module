/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.samv2.stub.SubstanceStub

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Ingredient(
	override val from: Long? = null,
	override val to: Long? = null,
	val rank: Int? = null,
	val type: IngredientType? = null,
	val knownEffect: Boolean? = null,
	val strengthDescription: String? = null,
	val strength: Quantity? = null,
	val additionalInformation: String? = null,
	val substance: SubstanceStub? = null
) : DataPeriod
