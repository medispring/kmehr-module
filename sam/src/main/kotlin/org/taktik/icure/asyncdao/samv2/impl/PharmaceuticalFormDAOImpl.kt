/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.PharmaceuticalFormDAO
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.samv2.PharmaceuticalForm

@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.PharmaceuticalForm') emit( null, doc._id )}",
)
class PharmaceuticalFormDAOImpl(
	couchDbDispatcher: CouchDbDispatcher,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	designDocumentProvider: DesignDocumentProvider,
) : InternalDAOImpl<PharmaceuticalForm>(
	entityClass = PharmaceuticalForm::class.java,
	couchDbDispatcher = couchDbDispatcher,
	datastoreInstanceProvider = datastoreInstanceProvider,
	designDocumentProvider = designDocumentProvider,
), PharmaceuticalFormDAO
