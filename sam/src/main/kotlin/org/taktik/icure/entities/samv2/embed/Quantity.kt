/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Quantity(val value: BigDecimal? = null, val unit: String? = null) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Quantity) return false

		if (value != null && other.value == null || value == null && other.value != null || (value?.compareTo(other.value) != 0 && value != other.value)) return false
		if (unit != other.unit) return false

		return true
	}

	override fun hashCode(): Int {
		var result = value?.toInt() ?: 0
		result = 31 * result + (unit?.hashCode() ?: 0)
		return result
	}
}
