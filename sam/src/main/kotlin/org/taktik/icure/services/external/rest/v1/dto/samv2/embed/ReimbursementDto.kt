/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import java.math.BigDecimal
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReimbursementDto(
	override val from: Long? = null,
	override val to: Long? = null,
	val deliveryEnvironment: DeliveryEnvironmentDto? = null,
	val code: String? = null,
	val codeType: DmppCodeTypeDto? = null,
	val multiple: MultipleTypeDto? = null,
	val temporary: Boolean? = null,
	val reference: Boolean? = null,
	val legalReferencePath: String? = null,
	val flatRateSystem: Boolean? = null,
	val reimbursementBasePrice: BigDecimal? = null,
	val referenceBasePrice: BigDecimal? = null,
	val copaymentSupplement: BigDecimal? = null,
	val pricingUnit: PricingDto? = null,
	val pricingSlice: PricingDto? = null,
	val reimbursementCriterion: ReimbursementCriterionDto? = null,
	val copayments: List<CopaymentDto>? = null
) : DataPeriodDto
