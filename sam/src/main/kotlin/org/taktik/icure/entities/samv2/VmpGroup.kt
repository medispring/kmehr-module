/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo
import org.taktik.icure.entities.samv2.embed.NoGenericPrescriptionReason
import org.taktik.icure.entities.samv2.embed.NoSwitchReason
import org.taktik.icure.entities.samv2.embed.Quantity
import org.taktik.icure.entities.samv2.embed.SamText
import org.taktik.icure.entities.samv2.embed.StandardDosage

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VmpGroup(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val from: Long? = null,
	val to: Long? = null,
	val code: String? = null,
	val name: SamText? = null,
	val noGenericPrescriptionReason: NoGenericPrescriptionReason? = null,
	val noSwitchReason: NoSwitchReason? = null,
	val standardDosage: List<StandardDosage>? = null,
	val singleAdministrationDose: Quantity? = null,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = emptyMap(),
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = emptyList(),
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = emptyList(),
) : StoredDocument {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
