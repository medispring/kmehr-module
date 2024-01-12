/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.samv2.Amp
import org.taktik.icure.entities.samv2.SamVersion

interface AmpDAO : InternalDAO<Amp> {
	fun findAmpsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?, pagination: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>
	fun findAmpsByVmpGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findAmpsByVmpGroupId(datastoreInformation: IDatastoreInformation, vmpgId: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findAmpsByVmpCode(datastoreInformation: IDatastoreInformation, vmpCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findAmpsByVmpId(datastoreInformation: IDatastoreInformation, vmpId: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findAmpsByDmppCode(datastoreInformation: IDatastoreInformation, dmppCode: String): Flow<ViewQueryResultEvent>
	fun findAmpsByAmpCode(datastoreInformation: IDatastoreInformation, ampCode: String): Flow<ViewQueryResultEvent>
	fun findAmpsByAtc(datastoreInformation: IDatastoreInformation, atc: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findAmpsByChapterParagraph(datastoreInformation: IDatastoreInformation, chapter: String, paragraph: String, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>

	fun listAmpIdsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?): Flow<String>
	fun listAmpIdsByVmpGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String): Flow<String>
	fun listAmpIdsByVmpGroupId(datastoreInformation: IDatastoreInformation, vmpgId: String): Flow<String>
	fun listAmpIdsByVmpCode(datastoreInformation: IDatastoreInformation, vmpCode: String): Flow<String>
	fun listAmpIdsByVmpId(datastoreInformation: IDatastoreInformation, vmpId: String): Flow<String>

	fun listAmpsByVmpGroupCodes(datastoreInformation: IDatastoreInformation, vmpgCodes: List<String>): Flow<Amp>
	fun listAmpsByDmppCodes(datastoreInformation: IDatastoreInformation, dmppCodes: List<String>): Flow<Amp>
	fun listAmpsByVmpGroupIds(datastoreInformation: IDatastoreInformation, vmpGroupIds: List<String>): Flow<Amp>
	fun listAmpsByVmpCodes(datastoreInformation: IDatastoreInformation, vmpCodes: List<String>): Flow<Amp>
	fun listAmpsByVmpIds(datastoreInformation: IDatastoreInformation, vmpIds: List<String>): Flow<Amp>

	suspend fun getVersion(datastoreInformation: IDatastoreInformation, ): SamVersion?
	suspend fun getProductIdsFromSignature(datastoreInformation: IDatastoreInformation, type: String): Map<String, String>
	suspend fun getSignature(datastoreInformation: IDatastoreInformation, clazz: String): SamVersion?
}
