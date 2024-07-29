package org.taktik.icure.asynclogic.bridge

import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.MessageLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement

@Service
class MessageLogicBridge : GenericLogicBridge<Message>(), MessageLogic {

    override suspend fun createMessage(message: Message): Message? {
        throw BridgeException()
    }

    override fun createMessages(entities: Collection<Message>): Flow<Message> {
        throw BridgeException()
    }

    override fun filterMessages(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<Message>
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findForHcPartySortedByReceived(
        hcPartyId: String,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override suspend fun addDelegation(message: Message, delegation: Delegation): Message? {
        throw BridgeException()
    }

    override suspend fun addDelegations(message: Message, delegations: List<Delegation>): Message? {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Message>> {
        throw BridgeException()
    }
    override suspend fun getMessage(messageId: String): Message? {
        throw BridgeException()
    }

    override fun getMessages(messageIds: List<String>): Flow<Message> {
        throw BridgeException()
    }

    override fun getMessageChildren(messageId: String): Flow<Message> {
        throw BridgeException()
    }

    override fun getMessagesByTransportGuids(hcpId: String, transportGuids: Set<String>): Flow<Message> {
        throw BridgeException()
    }

    override fun getMessagesChildren(parentIds: List<String>): Flow<List<Message>> {
        throw BridgeException()
    }

    override fun findMessagesByFromAddress(
        partyId: String,
        fromAddress: String,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun findMessagesByToAddress(
        partyId: String,
        toAddress: String,
        paginationOffset: PaginationOffset<ComplexKey>,
        reverse: Boolean
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun findMessagesByTransportGuidReceived(
        partyId: String,
        transportGuid: String?,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun findMessagesByTransportGuid(
        partyId: String,
        transportGuid: String?,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun listMessageIdsByTransportGuid(hcPartyId: String, transportGuid: String?): Flow<String> {
        throw BridgeException()
    }

    override fun findMessagesByTransportGuidSentDate(
        partyId: String,
        transportGuid: String,
        fromDate: Long,
        toDate: Long,
        paginationOffset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun listMessagesByExternalRefs(hcPartyId: String, externalRefs: List<String>): Flow<Message> {
        throw BridgeException()
    }

    override fun listMessagesByHCPartySecretPatientKeys(
        hcPartyId: String,
        secretPatientKeys: List<String>
    ): Flow<Message> {
        throw BridgeException()
    }

    override fun listMessageIdsByDataOwnerPatientSentDate(
        dataOwnerId: String,
        secretForeignKeys: Set<String>,
        startDate: Long?,
        endDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listMessagesByInvoiceIds(ids: List<String>): Flow<Message> {
        throw BridgeException()
    }

    override fun setStatus(messages: Collection<Message>, status: Int): Flow<Message> {
        throw BridgeException()
    }

    override fun setReadStatus(
        messages: Collection<Message>,
        userId: String?,
        status: Boolean,
        time: Long?
    ): Flow<Message> {
        throw BridgeException()
    }
}
