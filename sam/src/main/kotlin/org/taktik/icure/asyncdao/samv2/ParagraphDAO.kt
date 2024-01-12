/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.samv2.Paragraph

interface ParagraphDAO : InternalDAO<Paragraph> {
	fun findParagraphs(datastoreInformation: IDatastoreInformation, searchString: String, language: String, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>
	fun findParagraphsWithCnk(datastoreInformation: IDatastoreInformation, cnk: Long, language: String): Flow<Paragraph>
	suspend fun getParagraph(datastoreInformation: IDatastoreInformation, chapterName: String, paragraphName: String): Paragraph
}
