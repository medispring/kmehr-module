/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DmppDto(
	val id: String? = null,
	override val from: Long? = null,
	override val to: Long? = null,
	val deliveryEnvironment: DeliveryEnvironmentDto? = null,
	val code: String? = null,
	val codeType: DmppCodeTypeDto? = null,
	val price: String? = null,
	val cheap: Boolean? = null,
	val cheapest: Boolean? = null,
	val reimbursable: Boolean? = null,
	val reimbursements: List<ReimbursementDto>? = null,
	val productId: String? = null
) : DataPeriodDto
