/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.samv2.Vmp

interface VmpDAO : InternalDAO<Vmp> {
	fun findVmpsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?, pagination: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>
	fun findVmpsByVmpCode(datastoreInformation: IDatastoreInformation, vmpCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findVmpsByGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
	fun findVmpsByGroupId(datastoreInformation: IDatastoreInformation, vmpgId: String, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>

	fun listVmpIdsByGroupCode(datastoreInformation: IDatastoreInformation, vmpgCode: String): Flow<String>
	fun listVmpIdsByGroupId(datastoreInformation: IDatastoreInformation, vmpgId: String): Flow<String>
	fun listVmpIdsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?): Flow<String>

	fun listVmpsByVmpCodes(datastoreInformation: IDatastoreInformation, vmpCodes: List<String>): Flow<Vmp>
	fun listVmpsByGroupIds(datastoreInformation: IDatastoreInformation, vmpgIds: List<String>): Flow<Vmp>
}
