package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawContactApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.model.ListOfIds
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service as SpringService
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.ContactFilterMapper
import org.taktik.icure.asynclogic.bridge.mappers.ContactMapper
import org.taktik.icure.asynclogic.bridge.mappers.ServiceFilterMapper
import org.taktik.icure.asynclogic.bridge.mappers.ServiceMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.errors.UnauthorizedException

@SpringService
class ContactLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val contactMapper: ContactMapper,
    private val serviceMapper: ServiceMapper,
    private val contactFilterMapper: ContactFilterMapper,
    private val serviceFilterMapper: ServiceFilterMapper
) : GenericLogicBridge<Contact>(), ContactLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawContactApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    @OptIn(InternalIcureApi::class)
    override suspend fun createContact(contact: Contact): Contact? =
        getApi().createContact(contactMapper.map(contact)).successBody().let(contactMapper::map)

    override fun createContacts(contacts: Flow<Contact>): Flow<Contact> {
        throw BridgeException()
    }

    override suspend fun addDelegation(contactId: String, delegation: Delegation): Contact? {
        throw BridgeException()
    }

    override suspend fun addDelegations(contactId: String, delegations: List<Delegation>): Contact? {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<Contact>> {
        throw BridgeException()
    }

    override fun filterContacts(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<Contact>
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun filterServices(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<Service>
    ): Flow<Service> {
        throw BridgeException()
    }

    override fun listContactsByOpeningDate(
        hcPartyId: String,
        startOpeningDate: Long,
        endOpeningDate: Long,
        offset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun findContactsByIds(selectedIds: Collection<String>): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override suspend fun getContact(id: String): Contact? =
        getApi().getContact(id).successBodyOrNull404()?.let(contactMapper::map)

    override fun getContacts(selectedIds: Collection<String>): Flow<Contact> {
        throw BridgeException()
    }

    override suspend fun getServiceCodesOccurences(
        hcPartyId: String,
        codeType: String,
        minOccurences: Long
    ): List<LabelledOccurence> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override fun getServices(selectedServiceIds: Collection<String>): Flow<Service> = flow {
        emitAll(getApi()
            .getServices(ListOfIds(ids = selectedServiceIds.toList()))
            .successBody()
            .map(serviceMapper::map)
            .asFlow()
        )
    }

    override fun getServicesLinkedTo(
        ids: List<String>,
        linkType: String?
    ): Flow<Service> {
        throw BridgeException()
    }

    override fun listContactIdsByDataOwnerPatientOpeningDate(
        dataOwnerId: String,
        secretForeignKeys: Set<String>,
        startDate: Long?,
        endDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listContactsByExternalId(externalId: String): Flow<Contact> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override fun listContactsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<Contact> = flow {
        emitAll(getApi()
            .listContactsByHCPartyAndPatientForeignKeys(hcPartyId, ListOfIds(ids = secretPatientKeys))
            .successBody()
            .map(contactMapper::map)
            .asFlow()
        )
    }

    override fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact> {
        throw BridgeException()
    }

    override fun listContactsByHcPartyAndFormIds(hcPartyId: String, ids: List<String>): Flow<Contact> {
        throw BridgeException()
    }

    override fun listContactsByHcPartyServiceId(hcPartyId: String, serviceId: String): Flow<Contact> {
        throw BridgeException()
    }

    override fun listServicesByAssociationId(associationId: String): Flow<Service> {
        throw BridgeException()
    }

    override fun listServicesByHcPartyAndHealthElementIds(
        hcPartyId: String,
        healthElementIds: List<String>
    ): Flow<Service> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override fun modifyEntities(entities: Flow<Contact>): Flow<Contact> = flow {
        emitAll(
            getApi().modifyContacts(
                entities.map(contactMapper::map).toList()
            ).successBody().map(contactMapper::map).asFlow()
        )
    }

    @OptIn(InternalIcureApi::class)
    override fun matchEntitiesBy(filter: AbstractFilter<*>): Flow<String> = flow {
        contactFilterMapper.mapOrNull(filter)?.also {
            emitAll(getApi().matchContactsBy(it).successBody().asFlow())
        } ?: serviceFilterMapper.mapOrNull(filter)?.also {
            emitAll(getApi().matchServicesBy(it).successBody().asFlow())
        } ?: throw IllegalArgumentException("Unsupported filter ${filter::class.simpleName}")
    }

    override fun modifyEntities(entities: Collection<Contact>): Flow<Contact> = modifyEntities(entities.asFlow())
}
