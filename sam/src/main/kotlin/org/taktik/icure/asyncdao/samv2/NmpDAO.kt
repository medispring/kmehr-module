/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.samv2.Nmp

interface NmpDAO : InternalDAO<Nmp> {
	fun findNmpsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?, paginationOffset: PaginationOffset<List<String>>): Flow<ViewQueryResultEvent>
	fun listNmpIdsByLabel(datastoreInformation: IDatastoreInformation, language: String?, label: String?): Flow<String>
	fun listNmpsByCnks(datastoreInformation: IDatastoreInformation, cnks: List<String>): Flow<Nmp>
}
