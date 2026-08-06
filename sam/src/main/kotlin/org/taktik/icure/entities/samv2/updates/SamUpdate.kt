package org.taktik.icure.entities.samv2.updates

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.entity.Attachment
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.embed.RevisionInfo

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SamUpdate(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val samVersion: SamVersion,
	val version: String,
	val date: Int,
	val type: UpdateType,
	val updates: Map<BundleType, String> = emptyMap(),
	val deletions: Map<BundleType, String> = emptyMap(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = emptyMap(),
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = emptyList(),
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = emptyList(),
) : StoredDocument {

	enum class BundleType { Amps, NonMedicinals, Paragraphs, PharmaceuticalForms, Signatures, Substances, Verses, Vmps, VmpGroups }
	enum class SamVersion(val urlComponent: String) { V5("5"), V6("6") }

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}