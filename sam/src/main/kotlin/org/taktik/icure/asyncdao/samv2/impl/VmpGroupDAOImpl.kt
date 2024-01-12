/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.VmpGroupDAO
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.samv2.VmpGroup
import org.taktik.icure.utils.makeFromTo

@Repository("vmpGroupDAO")
@Profile("sam")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.VmpGroup' && !doc.deleted) emit( null, doc._id )}")
class VmpGroupDAOImpl(
	@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<VmpGroup>(VmpGroup::class.java, couchDbDispatcher, idGenerator, datastoreInstanceProvider, designDocumentProvider), VmpGroupDAO {
	@View(name = "by_groupcode", map = "classpath:js/vmpgroup/By_groupcode.js")
	override fun findVmpGroupsByVmpGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("by_groupcode", vmpgCode, vmpgCode, paginationOffset, false), String::class.java, Int::class.java, VmpGroup::class.java))
	}

	override fun listVmpGroupsByVmpGroupCodes(datastoreInformation: IDatastoreInformation, vmpgCodes: List<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocs<String, Int, VmpGroup>(
				createQuery("by_groupcode")
					.keys(vmpgCodes)
					.reduce(false)
					.includeDocs(true)
			).map { it.doc }
		)
	}

	override fun findVmpGroups(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<String>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(pagedViewQuery("all", null, "\ufff0", paginationOffset, false), String::class.java, Nothing::class.java, VmpGroup::class.java))
	}

	@View(name = "by_language_label", map = "classpath:js/vmpgroup/By_language_label.js")
	override fun findVmpGroupsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?, paginationOffset: PaginationOffset<List<String>>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			makeFromTo(label, language).let { (from, to) ->
				client.queryView(
					pagedViewQuery(
						"by_language_label",
						from,
						to,
						paginationOffset.toPaginationOffset { sk -> ComplexKey.of(*sk.mapIndexed { i, s -> if (i == 1) sanitizeString(s) else s }.toTypedArray()) },
						false
					),
					ComplexKey::class.java, String::class.java, VmpGroup::class.java
				)
			}
		)
	}

	override fun listVmpGroupIdsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			makeFromTo(label, language).let { (from, to) ->
				client.queryView<ComplexKey, String>(
					createQuery("by_language_label")
						.startKey(from)
						.endKey(to)
						.reduce(false)
						.includeDocs(false)
				).map { it.id }
			}
		)
	}
}
