/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.NmpDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeForSorting
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.samv2.Nmp
import org.taktik.icure.utils.makeFromTo
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.Nmp' && !doc.deleted) emit( null, doc._id )}")
class NmpDAOImpl(
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<Nmp>(
	entityClass = Nmp::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider
), NmpDAO {
	val cache = Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(1.minutes.toJavaDuration()).buildAsync<String, List<String>>()

	@View(name = "by_language_label", map = "classpath:js/nmp/By_language_label.js")
	override fun findNmpsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
		require(label != null && label.length >= 3) { "Label must be at least 3 characters long" }
		val rowIds = coroutineScope {
			//TODO check the relevance of using a SupervisorScope to avoid failure on parallel cancellation
			//TODO Use Pair<Long,Long> for key (SHA) and it
			cache.get(label) { key, _ ->
				future {
					val client = couchDbDispatcher.getClient(datastoreInformation)
					makeFromTo(key, language).let { (from, to) ->
						val viewQuery = createQuery("by_language_label")
							.startKey(from)
							.endKey(to)
							.reduce(false)
							.includeDocs(false)
						client.queryView<ComplexKey, String>(viewQuery).toList().sortedBy { sanitizeForSorting(it.value) }.map { it.id }
					}
				}
			}.await()
		}

		emitAll(getEntities(rowIds.asSequence().let { seq -> paginationOffset.startDocumentId?.let { start -> seq.dropWhile { it != start } } ?: seq }.take(paginationOffset.limit).toList()).map { ViewRowWithDoc(it.id, ComplexKey.of(language, ""), "", it) })
	}

	override fun listNmpIdsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val sanitizedLabel = label?.let { sanitizeString(it) }
		val from = ComplexKey.of(
			language ?: "\u0000",
			sanitizedLabel ?: "\u0000"
		)
		val to = ComplexKey.of(
			language ?: ComplexKey.emptyObject(),
			if (sanitizedLabel == null) ComplexKey.emptyObject() else sanitizedLabel + "\ufff0"
		)
		val viewQuery = createQuery("by_language_label")
			.startKey(from)
			.endKey(to)
			.reduce(false)
			.includeDocs(false)
		emitAll(client.queryView<ComplexKey, String>(viewQuery).map { it.id })
	}

	@View(name = "by_cnk", map = "classpath:js/nmp/By_cnk.js")
	override fun listNmpsByCnks(datastoreInformation: IDatastoreInformation, cnks: List<String>): Flow<Nmp> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery("by_cnk")
			.keys(cnks)
			.reduce(false)
			.includeDocs(true)
		emitAll(client.queryViewIncludeDocs<String, Int, Nmp>(viewQuery).map { it.doc })
	}
}
