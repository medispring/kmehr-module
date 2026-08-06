package org.taktik.icure.test.fake.components

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.AmpDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.utils.createQuery

class TestAmpDAO(
	private val ampDAO: AmpDAO,
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<Amp>(
	entityClass = Amp::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider
) {

	fun getAllAmps(): Flow<Amp> = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())

		val viewQuery = designDocumentProvider.createQuery(
			client,
			ampDAO, "all",
			entityClass
		).includeDocs(true)

		emitAll(client
			.queryViewIncludeDocs<Any?, String, Amp>(viewQuery)
			.map { it.doc }
		)
	}

}