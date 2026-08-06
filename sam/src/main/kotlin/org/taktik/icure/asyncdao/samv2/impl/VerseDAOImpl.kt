package org.taktik.icure.asyncdao.samv2.impl

import kotlinx.coroutines.flow.*
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.VerseDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.samv2.Verse

@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.Verse') emit( null, doc._id )}")
class VerseDAOImpl(
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider,
) : InternalDAOImpl<Verse>(
	entityClass = Verse::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider
), VerseDAO {
	@View(name = "by_chapter_paragraph", map = "classpath:js/verse/By_chapter_paragraph.js")
	override fun listVerses(
		datastoreInformation: IDatastoreInformation,
		chapterName: String,
		paragraphName: String,
	): Flow<Verse> =
		flow {
			val client = couchDbDispatcher.getClient(datastoreInformation)

			val viewQuery =
				createQuery("by_chapter_paragraph")
					.startKey(ComplexKey.of(chapterName, paragraphName, null, null))
					.endKey(ComplexKey.of(chapterName, paragraphName, ComplexKey.emptyObject(), ComplexKey.emptyObject()))
					.includeDocs(true)

			emitAll(client.queryViewIncludeDocs<ComplexKey, Int, Verse>(viewQuery).map { it.doc }.filter { it.endDate == null })
		}
}
