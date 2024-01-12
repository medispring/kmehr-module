/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.stub

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.samv2.embed.SamText

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VmpGroupStub(
	override val id: String,
	val code: String? = null,
	val name: SamText? = null
) : Identifiable<String>
