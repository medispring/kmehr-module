/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.samv2.stub

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.SamTextDto
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.StandardSubstanceDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SubstanceStubDto(
	val id: String? = null,

	val code: String? = null,
	val chemicalForm: String? = null,
	val name: SamTextDto? = null,
	val note: SamTextDto? = null,
	val standardSubstances: List<StandardSubstanceDto>? = null
) : Serializable
