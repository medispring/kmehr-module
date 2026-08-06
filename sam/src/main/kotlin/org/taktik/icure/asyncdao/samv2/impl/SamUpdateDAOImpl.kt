package org.taktik.icure.asyncdao.samv2.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.SamUpdateDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.samv2.updates.SamUpdate

@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.updates.SamUpdate') emit( null, doc._id )}",
	reduce = "function(keys, values, rereduce) { return values.reduce((maxValue, currentValue) => currentValue > maxValue ? currentValue : maxValue); }",
)
class SamUpdateDAOImpl(
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider,
) : InternalDAOImpl<SamUpdate>(
	entityClass = SamUpdate::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider
), SamUpdateDAO {
	override fun getEntities(): Flow<SamUpdate> =
		flow {
			val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
			emitAll(
				client
					.queryViewIncludeDocs<String?, String, SamUpdate>(
						createQuery("all")
							.reduce(false)
							.includeDocs(true)
							.descending(true),
					).map { it.doc },
			)
		}

	override suspend fun getLastAppliedUpdate(): SamUpdate? {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		val lastUpdateId =
			client
				.queryView<String?, String>(
					createQuery("all")
						.reduce(true)
						.groupLevel(1)
						.includeDocs(false),
				).firstOrNull()
				?.value
		return lastUpdateId?.let { get(it) }
	}
}
