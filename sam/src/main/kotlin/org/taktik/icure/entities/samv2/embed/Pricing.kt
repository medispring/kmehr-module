/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import java.io.Serializable
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Pricing(
	val quantity: BigDecimal? = null,
	val label: SamText? = null
) : Serializable {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Pricing) return false

		if (quantity != null && other.quantity == null || quantity == null && other.quantity != null || (quantity?.compareTo(other.quantity) != 0 && quantity != other.quantity)) return false
		if (label != other.label) return false

		return true
	}

	override fun hashCode(): Int {
		var result = quantity?.toInt() ?: 0
		result = 31 * result + (label?.hashCode() ?: 0)
		return result
	}
}
