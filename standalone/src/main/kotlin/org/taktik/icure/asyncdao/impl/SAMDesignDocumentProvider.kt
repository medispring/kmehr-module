package org.taktik.icure.asyncdao.impl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.Client
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.dao.designDocName
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.couchdb.support.DesignDocumentFactory
import org.taktik.icure.asyncdao.Partitions

@Profile("sam")
@Service
class SAMDesignDocumentProvider : DesignDocumentProvider {
    override fun baseDesignDocumentId(entityClass: Class<*>, secondaryPartition: String?) =
        designDocName(entityClass.simpleName, secondaryPartition)

    override suspend fun currentOrAvailableDesignDocumentId(
        client: Client,
        entityClass: Class<*>,
        metaDataSource: Any,
        secondaryPartition: String?
    ): String = baseDesignDocumentId(entityClass, secondaryPartition)

    override suspend fun generateDesignDocuments(
        entityClass: Class<*>,
        metaDataSource: Any,
        client: Client?, partition: Partitions, ignoreIfUnchanged: Boolean
    ): Set<DesignDocument> =
        DesignDocumentFactory.getStdDesignDocumentFactory().generateFrom(
            baseDesignDocumentId(entityClass),
            metaDataSource,
            useVersioning = false
        )

    override suspend fun generateExternalDesignDocuments(
        entityClass: Class<*>,
        partitionsWithRepo: Map<String, String>,
        client: Client?,
        ignoreIfUnchanged: Boolean
    ): Set<DesignDocument> = emptySet() // We don't need to generate external design documents in SAM


    override suspend fun currentDesignDocumentId(
        entityClass: Class<*>,
        metaDataSource: Any,
        secondaryPartition: String?
    ): String = baseDesignDocumentId(entityClass, secondaryPartition)

}
