package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.DocumentApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.domain.BatchUpdateDocumentInfo
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.mapper.DocumentV2Mapper
import org.taktik.icure.utils.asDataBuffer
import java.nio.ByteBuffer

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class DocumentLogicBridge(
    val asyncSessionLogic: BridgeAsyncSessionLogic,
    val bridgeConfig: BridgeConfig,
    private val documentMapper: DocumentV2Mapper
) : GenericLogicBridge<Document>(), DocumentLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        DocumentApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Document>> {
        throw BridgeException()
    }

    override suspend fun createDocument(document: Document, strict: Boolean): Document? =
        getApi()?.createDocument(
            documentMapper.map(document),
            strict
        )?.let(documentMapper::map)

    override fun createOrModifyDocuments(documents: List<BatchUpdateDocumentInfo>, strict: Boolean): Flow<Document> {
        throw BridgeException()
    }

    override fun getAttachment(documentId: String, attachmentId: String): Flow<ByteBuffer> {
        throw BridgeException()
    }

    override suspend fun getDocument(documentId: String): Document? =
        getApi()?.getDocument(documentId)?.let(documentMapper::map)

    override fun getDocuments(documentIds: Collection<String>): Flow<Document> {
        throw BridgeException()
    }

    override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev> {
        throw BridgeException()
    }

    override suspend fun getDocumentsByExternalUuid(documentId: String): List<Document> {
        throw BridgeException()
    }

    override suspend fun getMainAttachment(documentId: String): Flow<DataBuffer> =
        getApi()?.let { api ->
            val document = getDocument(documentId)
            if(document?.attachmentId != null || document?.objectStoreReference != null) {
                api.getDocumentMainAttachment(documentId, null).asDataBuffer()
            } else null
        } ?: emptyFlow()

    override suspend fun getMainAttachment(document: Document): Flow<DataBuffer> {
        throw BridgeException()
    }

    override fun listDocumentsByDocumentTypeHCPartySecretMessageKeys(
        documentTypeCode: String,
        hcPartyId: String,
        secretForeignKeys: List<String>
    ): Flow<Document> {
        throw BridgeException()
    }

    override fun listDocumentsByHCPartySecretMessageKeys(
        hcPartyId: String,
        secretForeignKeys: List<String>
    ): Flow<Document> {
        throw BridgeException()
    }

    override fun listDocumentIdsByDataOwnerPatientCreated(
        dataOwnerId: String,
        secretForeignKeys: Set<String>,
        startDate: Long?,
        endDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listDocumentsWithoutDelegation(limit: Int): Flow<Document> {
        throw BridgeException()
    }

    override suspend fun modifyDocument(updatedDocument: Document, strict: Boolean): Document? {
        throw BridgeException()
    }

    override suspend fun updateAttachments(
        currentDocument: Document,
        mainAttachmentChange: DataAttachmentChange?,
        secondaryAttachmentsChanges: Map<String, DataAttachmentChange>,
    ): Document? = getApi()?.let { api ->
        if (currentDocument.rev == null) throw IllegalStateException("Cannot update attachments of a document with null rev")
        when(mainAttachmentChange) {
            is DataAttachmentChange.CreateOrUpdate -> api.setDocumentAttachment(
                currentDocument.id,
                currentDocument.rev!!,
                mainAttachmentChange.utis,
                mainAttachmentChange.data.map { it.asByteBuffer() },
                mainAttachmentChange.size,
                null
            )
            is DataAttachmentChange.Delete -> api.deleteAttachment(currentDocument.id, currentDocument.rev!!)
            else -> currentDocument.let(documentMapper::map)
        }.let { initialDocument ->
            secondaryAttachmentsChanges.entries.fold(initialDocument) { doc, (key, update) ->
                when(update) {
                    is DataAttachmentChange.CreateOrUpdate -> api.setDocumentSecondaryAttachment(
                        doc.id,
                        doc.rev!!,
                        key,
                        update.utis,
                        update.data.map { it.asByteBuffer() },
                        update.size,
                        null
                    )
                    is DataAttachmentChange.Delete -> api.deleteSecondaryAttachment(doc.id, doc.rev!!, key)
                }
            }
        }.let(documentMapper::map)
    }
}
