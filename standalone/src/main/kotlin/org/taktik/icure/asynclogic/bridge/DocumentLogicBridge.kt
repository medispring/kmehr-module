package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawDocumentApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.DocumentLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.DocumentMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.asynclogic.objectstorage.DataAttachmentChange
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.domain.BatchUpdateDocumentInfo
import org.taktik.icure.entities.Document
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.utils.toByteArray
import java.nio.ByteBuffer

@Service
class DocumentLogicBridge(
    val asyncSessionLogic: BridgeAsyncSessionLogic,
    val bridgeConfig: BridgeConfig,
    private val documentMapper: DocumentMapper
) : GenericLogicBridge<Document>(), DocumentLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawDocumentApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Document>> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override suspend fun createDocument(document: Document, strict: Boolean): Document? =
        getApi().createDocument(
            documentMapper.map(document),
            strict
        ).successBody().let(documentMapper::map)

    override fun createOrModifyDocuments(documents: List<BatchUpdateDocumentInfo>, strict: Boolean): Flow<Document> {
        throw BridgeException()
    }

    override fun getAttachment(documentId: String, attachmentId: String): Flow<ByteBuffer> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override suspend fun getDocument(documentId: String): Document? =
        getApi().getDocument(documentId).successBody().let(documentMapper::map)

    override fun getDocuments(documentIds: Collection<String>): Flow<Document> {
        throw BridgeException()
    }

    override suspend fun getDocumentsByExternalUuid(documentId: String): List<Document> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override suspend fun getMainAttachment(documentId: String): Flow<DataBuffer> =
        getApi().let { api ->
            val document = getDocument(documentId)
            if(document?.attachmentId != null || document?.objectStoreReference != null) {
                val attachment = api.getMainAttachment(documentId, null).successBody()
                val bufferFactory = DefaultDataBufferFactory()
                val buffer = bufferFactory.allocateBuffer(attachment.size)
                buffer.write(attachment)
                flowOf(buffer)
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

    @OptIn(InternalIcureApi::class)
    override suspend fun updateAttachments(
        currentDocument: Document,
        mainAttachmentChange: DataAttachmentChange?,
        secondaryAttachmentsChanges: Map<String, DataAttachmentChange>,
    ): Document? = getApi().let { api ->
        if (currentDocument.rev == null) throw IllegalStateException("Cannot update attachments of a document with null rev")
        when(mainAttachmentChange) {
            is DataAttachmentChange.CreateOrUpdate -> api.setDocumentAttachment(
                documentId = currentDocument.id,
                rev = checkNotNull(currentDocument.rev) { "Rev cannot be null" },
                utis = mainAttachmentChange.utis,
                payload = mainAttachmentChange.data.toByteArray(true),
                lengthHeader = mainAttachmentChange.size,
                encrypted = null
            ).successBody()
            is DataAttachmentChange.Delete -> api.deleteAttachment(
                documentId = currentDocument.id,
                rev = checkNotNull(currentDocument.rev) { "Rev cannot be null" }
            ).successBody()
            else -> currentDocument.let(documentMapper::map)
        }.let { initialDocument ->
            secondaryAttachmentsChanges.entries.fold(initialDocument) { doc, (key, update) ->
                when(update) {
                    is DataAttachmentChange.CreateOrUpdate -> api.setSecondaryAttachment(
                        documentId = doc.id,
                        key = key,
                        rev = checkNotNull(currentDocument.rev) { "Rev cannot be null" },
                        utis = update.utis,
                        payload = update.data.toByteArray(true),
                        lengthHeader = update.size,
                        encrypted = null
                    ).successBody()
                    is DataAttachmentChange.Delete -> api.deleteSecondaryAttachment(
                        documentId = doc.id,
                        key = key,
                        rev = checkNotNull(currentDocument.rev) { "Rev cannot be null" }
                    ).successBody()
                }
            }
        }.let(documentMapper::map)
    }
}
