package org.taktik.icure.asynclogic.datastore.impl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.properties.SAMCouchDbProperties
import java.net.URI

@Service
@Profile("sam")
class SAMDatastoreInstanceProvider(
    private val couchDbProperties: SAMCouchDbProperties
): DatastoreInstanceProvider {
    override suspend fun getInstanceAndGroup(): IDatastoreInformation =
        SAMDatastoreInformation(URI(couchDbProperties.url))
}
