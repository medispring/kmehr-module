/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.AmpDAO
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.entities.samv2.SamVersion
import org.taktik.icure.utils.makeFromTo
import org.taktik.icure.utils.toInputStream
import java.util.zip.GZIPInputStream

@Repository("ampDAO")
@Profile("sam")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.Amp' && !doc.deleted) emit( null, doc._id )}")
class AmpDAOImpl(
	@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<Amp>(Amp::class.java, couchDbDispatcher, idGenerator, datastoreInstanceProvider, designDocumentProvider), AmpDAO {

	companion object {
		const val ampPaginationLimit = 101
	}

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

	override fun listAmpIdsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val (from, to) = makeFromTo(label, language)
		val viewQuery = createQuery("by_language_label")
			.startKey(from)
			.endKey(to)
			.reduce(false)
			.includeDocs(false)
		emitAll(client.queryView<ComplexKey, String>(viewQuery).map { it.id })
	}
}
