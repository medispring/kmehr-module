/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import java.io.Serializable
import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class NumeratorRangeDto(val min: BigDecimal? = null, val max: BigDecimal? = null, val unit: String? = null) : Serializable
