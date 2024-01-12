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
import org.taktik.icure.asyncdao.samv2.SubstanceDAO
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.samv2.Substance

@Repository("substanceDAO")
@Profile("sam")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.samv2.embed.Substance') emit( null, doc._id )}")
class SubstanceDAOImpl(
    @Qualifier("drugCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
    idGenerator: IDGenerator,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    designDocumentProvider: DesignDocumentProvider
) : InternalDAOImpl<Substance>(Substance::class.java, couchDbDispatcher, idGenerator, datastoreInstanceProvider, designDocumentProvider), SubstanceDAO
