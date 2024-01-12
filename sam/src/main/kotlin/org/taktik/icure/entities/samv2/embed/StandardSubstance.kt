/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class StandardSubstance(
	val code: String? = null,
	val type: StandardSubstanceType? = null,
	val name: SamText? = null,
	val definition: SamText? = null,
	val url: String? = null
) : Serializable
