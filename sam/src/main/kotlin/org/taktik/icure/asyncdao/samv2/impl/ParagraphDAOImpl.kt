package org.taktik.icure.asyncdao.samv2.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.AmpDAO
import org.taktik.icure.asyncdao.samv2.ParagraphDAO
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.samv2.Paragraph
import org.taktik.icure.utils.distinct
import java.text.DecimalFormat

@Repository("paragraphDAO")
@Profile("sam")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.Paragraph') emit( null, doc._id )}")
class ParagraphDAOImpl(
	@Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider,
	val ampDAO: AmpDAO
) : InternalDAOImpl<Paragraph>(Paragraph::class.java, couchDbDispatcher, idGenerator, datastoreInstanceProvider, designDocumentProvider), ParagraphDAO {
	@View(name = "by_language_label", map = "classpath:js/paragraph/By_language_label.js")
	override fun findParagraphs(datastoreInformation: IDatastoreInformation, searchString: String, language: String, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val sanitizedLabel = sanitizeString(searchString)
		val viewQuery = pagedViewQuery(
			"by_language_label",
			ComplexKey.of(
				language,
				sanitizedLabel
			),
			ComplexKey.of(
				language,
				sanitizedLabel + "\ufff0"
			),
			paginationOffset.toPaginationOffset { sk -> ComplexKey.of(*sk.mapIndexed { i, s -> if (i == 1) sanitizeString(s) else s }.toTypedArray()) },
			false
		)
		emitAll(client.queryView(viewQuery, ComplexKey::class.java, String::class.java, Paragraph::class.java).filter { it !is ViewRowWithDoc<*, *, *> || (it.doc as Paragraph).endDate == null })
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@View(name = "by_chapter_paragraph", map = "classpath:js/paragraph/By_chapter_paragraph.js")
	override fun findParagraphsWithCnk(datastoreInformation: IDatastoreInformation, cnk: Long, language: String): Flow<Paragraph> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val legalReferences = ampDAO.listAmpsByDmppCodes(datastoreInformation, listOf(DecimalFormat("0000000").format(cnk))).flatMapConcat { amp ->
			amp.ampps.flatMap { ampp ->
				ampp.dmpps.flatMap { dmpp -> (dmpp.reimbursements ?: emptySet()).mapNotNull { it.legalReferencePath } }
			}.asFlow()
		}.mapNotNull { legalReferencePath ->
			legalReferencePath.split("-").takeIf { it.size == 3 && it[1] == "IV" }?.let { ComplexKey.of("IV", it[2]) }
		}.distinct().toList()

		val viewQuery = createQuery("by_chapter_paragraph")
			.keys(legalReferences)
			.includeDocs(true)
		emitAll(client.queryViewIncludeDocs<ComplexKey, Int, Paragraph>(viewQuery).map { it.doc }.filter { it.endDate == null })
	}

	override suspend fun getParagraph(datastoreInformation: IDatastoreInformation, chapterName: String, paragraphName: String): Paragraph {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery("by_chapter_paragraph")
			.startKey(ComplexKey.of(chapterName, paragraphName))
			.endKey(ComplexKey.of(chapterName, paragraphName))
			.includeDocs(true)

		return client.queryViewIncludeDocs<String, Int, Paragraph>(viewQuery).map { it.doc }.first { it.endDate == null }
	}
}
