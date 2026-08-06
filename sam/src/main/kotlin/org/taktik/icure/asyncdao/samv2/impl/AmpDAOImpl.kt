/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.AmpDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeForSorting
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.entities.samv2.SamVersion
import org.taktik.icure.exceptions.TooManyResultsException
import org.taktik.icure.utils.makeFromTo
import org.taktik.icure.utils.toInputStream
import java.util.zip.GZIPInputStream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.Amp' && !doc.deleted) emit( null, doc._id )}")
class AmpDAOImpl(
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<Amp>(
	entityClass = Amp::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider
), AmpDAO {

	companion object {
		const val ampPaginationLimit = 101
	}

	private val amppCache = Caffeine.newBuilder()
		.maximumWeight(1_000_000)
		.weigher { _: String, value: List<Pair<String, String>> -> value.size }
		.expireAfterAccess(2.minutes.toJavaDuration())
		.buildAsync<String, List<Pair<String, String>>>()

	private val ampCache = Caffeine.newBuilder()
		.maximumWeight(1_000_000)
		.weigher { _: String, value: List<String> -> value.size }
		.expireAfterAccess(2.minutes.toJavaDuration())
		.buildAsync<String, List<String>>()

	@View(name = "by_dmppcode", map = "classpath:js/amp/By_dmppcode.js")
	override fun findAmpsByDmppCode(datastoreInformation: IDatastoreInformation, dmppCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView(
				createQuery("by_dmppcode")
					.startKey(dmppCode)
					.endKey(dmppCode)
					.includeDocs(true)
					.limit(ampPaginationLimit),
				String::class.java, String::class.java, Amp::class.java
			)
		)
	}

	@View(name = "by_ampcode", map = "classpath:js/amp/By_ampcode.js")
	override fun findAmpsByAmpCode(datastoreInformation: IDatastoreInformation, ampCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView(
				createQuery("by_ampcode")
					.startKey(ampCode)
					.endKey(ampCode)
					.includeDocs(true)
					.limit(ampPaginationLimit),
				String::class.java, String::class.java, Amp::class.java
			)
		)
	}

	@View(name = "by_groupcode", map = "classpath:js/amp/By_groupcode.js")
	override fun findAmpsByVmpGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("by_groupcode", vmpgCode, vmpgCode, paginationOffset.copy(limit = paginationOffset.limit.coerceAtMost(ampPaginationLimit)), false), String::class.java, String::class.java, Amp::class.java))
	}

	@View(name = "by_atc", map = "classpath:js/amp/By_atc.js")
	override fun findAmpsByAtc(datastoreInformation: IDatastoreInformation, atc: String, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("by_atc", atc, atc, paginationOffset.copy(limit = paginationOffset.limit.coerceAtMost(ampPaginationLimit)), false), String::class.java, String::class.java, Amp::class.java))
	}

	@View(name = "by_groupid", map = "classpath:js/amp/By_groupid.js")
	override fun findAmpsByVmpGroupId(datastoreInformation: IDatastoreInformation, vmpgId: String, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("by_groupid", vmpgId, vmpgId, paginationOffset.copy(limit = paginationOffset.limit.coerceAtMost(ampPaginationLimit)), false), String::class.java, String::class.java, Amp::class.java))
	}

	@View(name = "by_vmpcode", map = "classpath:js/amp/By_vmpcode.js")
	override fun findAmpsByVmpCode(datastoreInformation: IDatastoreInformation, vmpCode: String, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("by_vmpcode", vmpCode, vmpCode, paginationOffset.copy(limit = paginationOffset.limit.coerceAtMost(ampPaginationLimit)), false), String::class.java, String::class.java, Amp::class.java))
	}

	@View(name = "by_vmpid", map = "classpath:js/amp/By_vmpid.js")
	override fun findAmpsByVmpId(datastoreInformation: IDatastoreInformation, vmpId: String, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("by_vmpid", vmpId, vmpId, paginationOffset, false), String::class.java, String::class.java, Amp::class.java))
	}

	override fun listAmpIdsByVmpGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView<String, String>(
				createQuery("by_groupcode")
					.startKey(vmpgCode)
					.endKey(vmpgCode)
					.reduce(false)
					.includeDocs(false)
			).map { it.id }
		)
	}

	override fun listAmpIdsByVmpGroupId(datastoreInformation: IDatastoreInformation, vmpgId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView<String, String>(
				createQuery("by_groupid")
					.startKey(vmpgId)
					.endKey(vmpgId)
					.reduce(false)
					.includeDocs(false)
			).map { it.id }
		)
	}

	override fun listAmpIdsByVmpCode(datastoreInformation: IDatastoreInformation, vmpCode: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView<String, String>(
				createQuery("by_code")
					.startKey(vmpCode)
					.endKey(vmpCode)
					.reduce(false)
					.includeDocs(false)
			).map { it.id }
		)
	}

	override fun listAmpIdsByVmpId(datastoreInformation: IDatastoreInformation, vmpId: String) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView<String, String>(
				createQuery("by_id")
					.startKey(vmpId)
					.endKey(vmpId)
					.reduce(false)
					.includeDocs(false)
			).map { it.id }
		)
	}

	override fun listAmpsByVmpGroupCodes(datastoreInformation: IDatastoreInformation, vmpgCodes: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<String, Int, Amp>(
				createQuery("by_groupcode")
					.keys(vmpgCodes)
					.reduce(false)
					.includeDocs(true)
					.limit(ampPaginationLimit)
			).map { it.doc }
		)
	}

	override fun listAmpsByDmppCodes(datastoreInformation: IDatastoreInformation, dmppCodes: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<String, Int, Amp>(
				createQuery("by_dmppcode")
					.keys(dmppCodes)
					.reduce(false)
					.includeDocs(true)
					.limit(ampPaginationLimit)
			).map { it.doc }
		)
	}

	override fun listAmpsByVmpGroupIds(datastoreInformation: IDatastoreInformation, vmpGroupIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<String, Int, Amp>(
				createQuery("by_groupid")
					.keys(vmpGroupIds)
					.reduce(false)
					.includeDocs(true)
					.limit(ampPaginationLimit)
			).map { it.doc }
		)
	}

	override fun listAmpsByVmpCodes(datastoreInformation: IDatastoreInformation, vmpCodes: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<String, Int, Amp>(
				createQuery("by_vmpcode")
					.keys(vmpCodes)
					.reduce(false)
					.includeDocs(true)
					.limit(ampPaginationLimit)
			).map { it.doc }
		)
	}

	override fun listAmpsByVmpIds(datastoreInformation: IDatastoreInformation, vmpIds: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<String, Int, Amp>(
				createQuery("by_vmpid")
					.keys(vmpIds)
					.reduce(false)
					.includeDocs(true)
					.limit(ampPaginationLimit)
			).map { it.doc }
		)
	}

	override suspend fun getVersion(datastoreInformation: IDatastoreInformation) =
		couchDbDispatcher.getClient(datastoreInformation).get("org.taktik.icure.samv2", SamVersion::class.java)

	override suspend fun getSignature(datastoreInformation: IDatastoreInformation, clazz: String) =
		couchDbDispatcher.getClient(datastoreInformation).get("org.taktik.icure.samv2.signatures.$clazz", SamVersion::class.java)

	override suspend fun getProductIdsFromSignature(datastoreInformation: IDatastoreInformation, type: String): Map<String, String> {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		return client.get("org.taktik.icure.samv2.signatures.$type", SamVersion::class.java)?.let { samVersion ->
			GZIPInputStream(client.getAttachment(samVersion.id, "signatures", samVersion.rev).toInputStream()).reader(Charsets.UTF_8).useLines {
				it.fold(HashMap()) { acc, l -> acc.also { l.split('|').let { parts -> acc[parts[0]] = parts[1] } } }
			}
		} ?: throw IllegalArgumentException("Signature $type does not exist")
	}

	@View(name = "by_language_label", map = "classpath:js/amp/By_language_label.js")
	override fun findAmpsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?, pagination: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val (from, to) = makeFromTo(label, language)
		val viewQuery = pagedViewQuery(
			"by_language_label",
			from,
			to,
			pagination.toPaginationOffset { sk -> ComplexKey.of(*sk.mapIndexed { i, s -> if (i == 1) sanitizeString(s) else s }.toTypedArray()) }.let {
			  it.copy(limit = it.limit.coerceAtMost(ampPaginationLimit))
			},
			false
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, String::class.java, Amp::class.java))
	}

	@View(name = "by_chapter_paragraph", map = "classpath:js/amp/By_chapter_paragraph.js")
	override fun findAmpsByChapterParagraph(datastoreInformation: IDatastoreInformation, chapter: String, paragraph: String, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			"by_chapter_paragraph",
			ComplexKey.of(chapter, paragraph),
			ComplexKey.of(chapter, paragraph),
			paginationOffset.toPaginationOffset { sk -> ComplexKey.of(*sk.mapIndexed { i, s -> if (i == 1) sanitizeString(s) else s }.toTypedArray()) },
			false
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, Int::class.java, Amp::class.java))
	}

	@View(name = "by_language_label_official_sort", map = "classpath:js/amp/By_language_label_official_sort.js", secondaryPartition = "official-sort")
	override fun listAmpAmppIdsByLabel(
		datastoreInformation: IDatastoreInformation,
		language: String,
		label: String
	): Flow<Pair<String, String>> = flow {
		require(label.length >= 3) { "Label must be at least 3 characters long" }
		val rowIds = coroutineScope {
			//TODO check the relevance of using a SupervisorScope to avoid failure on parallel cancellation
			//TODO Use Pair<Long,Long> for key (SHA) and it
			//NOTE: the datastoreInformation always point to the same db and must not be part of the cache key
			amppCache.get(label) { key, _ ->
				future {
					val client = couchDbDispatcher.getClient(datastoreInformation)
					makeFromTo(key, language).let { (from, to) ->
						val viewQuery = createQuery("by_language_label_official_sort", "official-sort")
							.startKey(from)
							.endKey(to)
							.reduce(false)
							.limit(10001)
							.includeDocs(false)
						client.queryView<ComplexKey, AmppRef>(viewQuery)
							.mapNotNull { it.value?.let { value -> ViewRowNoDoc(it.id, it.key, AmppRef(
								value.vmpGroupName,
								value.index,
								value.name?.let { sanitizeForSorting(it) },
								value.ctiExtended)
							) } }.toList()
							.also { if (it.size > 10000) throw TooManyResultsException("Too many results for label '$label', please provide a more precise label") }
							.sortedWith(compareBy({it.value?.vmpGroupName}, {it.value?.index}, {it.value?.name}))
							.mapNotNull { it.value?.ctiExtended?.let { ctiExtended -> it.id to ctiExtended } }
					}
				}
			}.await()
		}
		emitAll(rowIds.asFlow())	}


	override fun listAmpIdsByLabel(datastoreInformation: IDatastoreInformation, language: String, label: String): Flow<String> = flow {
		require(label.length >= 3) { "Label must be at least 3 characters long" }
		val rowIds = coroutineScope {
			//TODO check the relevance of using a SupervisorScope to avoid failure on parallel cancellation
			//TODO Use Pair<Long,Long> for key (SHA) and it
			//NOTE: the datastoreInformation always point to the same db and must not be part of the cache key
			ampCache.get(label) { key, _ ->
				future {
					val client = couchDbDispatcher.getClient(datastoreInformation)
					makeFromTo(key, language).let { (from, to) ->
						val viewQuery = createQuery("by_language_label")
							.startKey(from)
							.endKey(to)
							.reduce(false)
							.limit(10001)
							.includeDocs(false)
						client.queryView<ComplexKey, String>(viewQuery).map { ViewRowNoDoc(it.id, it.key, sanitizeForSorting(it.value)) }.toList()
							.also { if (it.size > 10000) throw TooManyResultsException("Too many results for label '$label', please provide a more precise label") }
							.sortedBy { it.value }.map { it.id }
					}
				}
			}.await()
		}
		emitAll(rowIds.asFlow())
	}
}

class AmppRefDeserializer : JsonDeserializer<AmppRef>() {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): AmppRef {
		val array = p.codec.readTree<com.fasterxml.jackson.databind.node.ArrayNode>(p)
		check(array.size() == 4) { "AmppRef array must have exactly 4 elements" }

		val vmpGroupName = if (array[0].isNull) null else array[0].asText()
		val index = array[1].asDouble()
		val name = if (array[2].isNull) null else array[2].asText()
		val ctiExtended = if (array[3].isNull) null else array[3].asText()

		return AmppRef(vmpGroupName, index, name, ctiExtended)
	}
}

@JsonDeserialize(using = AmppRefDeserializer::class)
data class AmppRef(
	val vmpGroupName: String? = null,
	val index: Double,
	val name: String?,
	val ctiExtended: String?
)
