/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.VmpGroupDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeForSorting
import org.taktik.icure.entities.samv2.VmpGroup
import org.taktik.icure.utils.makeFromTo
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.VmpGroup' && !doc.deleted) emit( null, doc._id )}",
)
class VmpGroupDAOImpl(
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider,
) : InternalDAOImpl<VmpGroup>(
	entityClass = VmpGroup::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider
), VmpGroupDAO {
	val cache =
		Caffeine
			.newBuilder()
			.maximumSize(1000)
			.expireAfterAccess(1.minutes.toJavaDuration())
			.buildAsync<String, List<String>>()

	@View(name = "by_groupcode", map = "classpath:js/vmpgroup/By_groupcode.js")
	override fun findVmpGroupsByVmpGroupCode(
		datastoreInformation: IDatastoreInformation,
		vmpgCode: String,
		paginationOffset: PaginationOffset<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView(
				pagedViewQuery("by_groupcode", vmpgCode, vmpgCode, paginationOffset, false),
				String::class.java,
				Int::class.java,
				VmpGroup::class.java,
			),
		)
	}

	override fun listVmpGroupsByVmpGroupCodes(
		datastoreInformation: IDatastoreInformation,
		vmpgCodes: List<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client
				.queryViewIncludeDocs<String, Int, VmpGroup>(
					createQuery("by_groupcode")
						.keys(vmpgCodes)
						.reduce(false)
						.includeDocs(true),
				).map { it.doc },
		)
	}

	override fun findVmpGroups(
		datastoreInformation: IDatastoreInformation,
		paginationOffset: PaginationOffset<String>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryView(
				pagedViewQuery("all", null, "\ufff0", paginationOffset, false),
				String::class.java,
				Nothing::class.java,
				VmpGroup::class.java,
			),
		)
	}

	@View(name = "by_language_label", map = "classpath:js/vmpgroup/By_language_label.js")
	override fun findVmpGroupsByLabel(
		datastoreInformation: IDatastoreInformation,
		language: String?,
		label: String?,
		paginationOffset: PaginationOffset<List<String>>,
	) = flow {
		require(label != null && label.length >= 3) { "Label must be at least 3 characters long" }
		val rowIds =
			coroutineScope {
				// TODO check the relevance of using a SupervisorScope to avoid failure on parallel cancellation
				// TODO Use Pair<Long,Long> for key (SHA) and it
				cache
					.get(label) { key, _ ->
						future {
							val client = couchDbDispatcher.getClient(datastoreInformation)
							makeFromTo(key, language).let { (from, to) ->
								val viewQuery =
									createQuery("by_language_label")
										.startKey(from)
										.endKey(to)
										.reduce(false)
										.includeDocs(false)
								client
									.queryView<ComplexKey, String>(viewQuery)
									.toList()
									.sortedBy { sanitizeForSorting(it.value) }
									.map { it.id }
							}
						}
					}.await()
			}

		emitAll(
			getEntities(
				rowIds
					.asSequence()
					.let { seq ->
						paginationOffset.startDocumentId?.let { start -> seq.dropWhile { it != start } }
							?: seq
					}.take(paginationOffset.limit)
					.toList(),
			).map { ViewRowWithDoc(it.id, ComplexKey.of(language, ""), "", it) },
		)
	}

	override fun listVmpGroupIdsByLabel(
		datastoreInformation: IDatastoreInformation,
		language: String?,
		label: String?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			makeFromTo(label, language).let { (from, to) ->
				client
					.queryView<ComplexKey, String>(
						createQuery("by_language_label")
							.startKey(from)
							.endKey(to)
							.reduce(false)
							.includeDocs(false),
					).map { it.id }
			},
		)
	}
}
