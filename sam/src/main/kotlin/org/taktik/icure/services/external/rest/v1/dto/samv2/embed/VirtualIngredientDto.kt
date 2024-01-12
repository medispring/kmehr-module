/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.samv2.stub.SubstanceStubDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VirtualIngredientDto(
	override val from: Long? = null,
	override val to: Long? = null,
	val rank: Int? = null,
	val type: IngredientTypeDto? = null,
	val strengthRange: StrengthRangeDto? = null,
	val substance: SubstanceStubDto? = null
) : DataPeriodDto
