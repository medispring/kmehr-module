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
import org.taktik.icure.entities.samv2.embed.*
import org.taktik.icure.entities.samv2.stub.VmpGroupStub

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Vmp(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val from: Long? = null,
	val to: Long? = null,
	val code: String? = null,
	val vmpGroup: VmpGroupStub? = null,
	val singleAdministrationDose: Quantity? = null,
	val name: SamText? = null,
	val abbreviation: SamText? = null,
	val vtm: Vtm? = null,
	val wadas: Set<Wada>? = null,
	val components: Set<VmpComponent>? = null,
	val commentedClassifications: Set<CommentedClassification>? = null,

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = emptyMap(),
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = emptyList(),
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = emptyList(),
) : StoredDocument {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
