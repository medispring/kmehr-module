package org.taktik.icure.asynclogic.datastore.impl

import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import java.net.URI

data class SAMDatastoreInformation(
    val dbInstanceUrl: URI
) : IDatastoreInformation {
    override fun getFullIdFor(entityId: String): String = "FALLBACK:$entityId"

}
