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
import org.taktik.icure.entities.samv2.stub.VmpStub
import java.time.Duration

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Amp(
	@param:JsonProperty("_id") override val id: String,
	@param:JsonProperty("_rev") override val rev: String? = null,
	@param:JsonProperty("deleted") override val deletionDate: Long? = null,

	val from: Long? = null,
	val to: Long? = null,
	val code: String? = null,
	val vmp: VmpStub? = null,
	val officialName: String? = null,
	val status: AmpStatus? = null,
	val name: SamText? = null,
	val blackTriangle: Boolean = false,
	val medicineType: MedicineType? = null,
	val company: Company? = null,
	val abbreviatedName: SamText? = null,
	val proprietarySuffix: SamText? = null,
	val prescriptionName: SamText? = null,
	val ampps: Set<Ampp> = emptySet(),
	val components: Set<AmpComponent> = emptySet(),

	@param:JsonProperty("_attachments") override val attachments: Map<String, Attachment>? = emptyMap(),
	@param:JsonProperty("_revs_info") override val revisionsInfo: List<RevisionInfo>? = emptyList(),
	@param:JsonProperty("_conflicts") override val conflicts: List<String>? = emptyList(),
) : StoredDocument {

	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	fun hasValidAmpps(includeWithoutCommercializations: Boolean, includeFutureAmpps: Boolean = false): Boolean {
		val now = System.currentTimeMillis()
		val twoYearsAgo = now - Duration.ofDays(365 * 2).toMillis()
		return from != null && from < now && (to == null || to > now) && ampps.any {
			if (includeFutureAmpps) it.isOrWillBeValid(now, twoYearsAgo, includeWithoutCommercializations) else it.isValid(now, twoYearsAgo, includeWithoutCommercializations)
		}
	}

	fun removeInvalidAmpps(includeWithoutCommercializations: Boolean, includeFutureAmpps: Boolean = false): Amp {
		val now = System.currentTimeMillis()
		val twoYearsAgo = now - Duration.ofDays(365 * 2).toMillis()
		return copy(
			ampps = ampps.filter { if (includeFutureAmpps) it.isOrWillBeValid(now, twoYearsAgo, includeWithoutCommercializations) else it.isValid(now, twoYearsAgo, includeWithoutCommercializations) }.toSet()
		)
	}
}
