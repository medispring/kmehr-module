/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.samv2.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.id.IDGenerator
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.impl.InternalDAOImpl
import org.taktik.icure.asyncdao.samv2.ProductIdDAO
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.samv2.ProductId

@Repository("productIdDAO")
@Profile("sam")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.ProductId') emit( null, doc._id )}")
class ProductIdDAOImpl(
    @Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
    idGenerator: IDGenerator,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<ProductId>(ProductId::class.java, couchDbDispatcher, idGenerator, datastoreInstanceProvider, designDocumentProvider), ProductIdDAO
