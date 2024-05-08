package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.ContactApi
import io.icure.kraken.client.security.ExternalJWTProvider
import io.jsonwebtoken.JwtException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.mapper.ContactV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.ServiceV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class ContactLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val contactMapper: ContactV2Mapper,
    private val serviceMapper: ServiceV2Mapper
) : GenericLogicBridge<Contact>(), ContactLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        ContactApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }

    override suspend fun createContact(contact: Contact): Contact? =
        getApi()?.createContact(contactMapper.map(contact))?.let(contactMapper::map)

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
        filter: FilterChain<org.taktik.icure.entities.embed.Service>
    ): Flow<org.taktik.icure.entities.embed.Service> {
        throw BridgeException()
    }

    override fun solveConflicts(limit: Int?): Flow<IdAndRev> {
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

    override suspend fun getContact(id: String): Contact? =
        getApi()?.getContact(id)?.let(contactMapper::map)

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

    override fun getServices(selectedServiceIds: Collection<String>): Flow<org.taktik.icure.entities.embed.Service> = flow {
        emitAll(
            getApi()
                ?.getServices(ListOfIdsDto(ids = selectedServiceIds.toList()))
                ?.map(serviceMapper::map)
                ?.asFlow() ?: emptyFlow()
        )
    }


    override fun getServicesLinkedTo(
        ids: List<String>,
        linkType: String?
    ): Flow<org.taktik.icure.entities.embed.Service> {
        throw BridgeException()
    }

    override fun listContactIds(hcPartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listContactIdsByCode(
        hcPartyId: String,
        codeType: String,
        codeCode: String,
        startValueDate: Long?,
        endValueDate: Long?
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listContactIdsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<String> {
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

    override fun listContactIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String> {
        throw BridgeException()
    }

    override fun listContactIdsByTag(
        hcPartyId: String,
        tagType: String,
        tagCode: String,
        startValueDate: Long?,
        endValueDate: Long?
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listContactsByExternalId(externalId: String): Flow<Contact> {
        throw BridgeException()
    }

    override fun listContactsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<Contact> = flow {
        emitAll(
            getApi()
                ?.listContactsByHCPartyAndPatientForeignKeys(hcPartyId, ListOfIdsDto(ids = secretPatientKeys))
                ?.map(contactMapper::map)
                ?.asFlow() ?: emptyFlow()
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

    override fun listIdsByServices(services: Collection<String>): Flow<String> {
        throw BridgeException()
    }

    override fun listServiceIdsByCode(
        hcPartyId: String,
        patientSecretForeignKeys: List<String>?,
        codeType: String,
        codeCode: String,
        startValueDate: Long?,
        endValueDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listServiceIdsByHcParty(hcPartyId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listServiceIdsByHcPartyAndHealthElementIds(
        hcPartyId: String,
        healthElementIds: List<String>
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listServiceIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String> {
        throw BridgeException()
    }

    override fun listServiceIdsByTag(
        hcPartyId: String,
        patientSecretForeignKeys: List<String>?,
        tagType: String,
        tagCode: String,
        startValueDate: Long?,
        endValueDate: Long?,
        descending: Boolean
    ): Flow<String> = flow {
        patientSecretForeignKeys?.forEach { foreignKey ->
            emitAll(
                listServiceIdsByTagRecursive(
                    hcPartyId,
                    foreignKey,
                    tagType,
                    tagCode,
                    startValueDate,
                    endValueDate,
                    null
                )
            )
        }
    }

    private fun listServiceIdsByTagRecursive(
        hcPartyId: String,
        patientSecretForeignKey: String?,
        tagType: String,
        tagCode: String,
        startValueDate: Long?,
        endValueDate: Long?,
        startDocumentId: String? = null
    ): Flow<String> = flow {
        val result = getApi()?.filterServicesBy(
            io.icure.kraken.client.models.filter.chain.FilterChain(
                filter = io.icure.kraken.client.models.filter.service.ServiceByHcPartyTagCodeDateFilter(
                    healthcarePartyId = hcPartyId,
                    patientSecretForeignKey = patientSecretForeignKey,
                    tagType = tagType,
                    tagCode = tagCode,
                    startValueDate = startValueDate,
                    endValueDate = endValueDate
                )
            ),
            startDocumentId = startDocumentId,
            limit = 1000
        ) ?: throw JwtException("Invalid JWT")
        emitAll(result.rows.map { it.id }.asFlow())
        if(result.nextKeyPair?.startKeyDocId != null) {
            emitAll(
                listServiceIdsByTagRecursive(
                    hcPartyId,
                    patientSecretForeignKey,
                    tagType,
                    tagCode,
                    startValueDate,
                    endValueDate,
                    result.nextKeyPair?.startKeyDocId
                )
            )
        }
    }

    override fun listServicesByAssociationId(associationId: String): Flow<org.taktik.icure.entities.embed.Service> {
        throw BridgeException()
    }

    override fun listServicesByHcPartyAndHealthElementIds(
        hcPartyId: String,
        healthElementIds: List<String>
    ): Flow<org.taktik.icure.entities.embed.Service> {
        throw BridgeException()
    }

    override fun listServicesByHcPartyAndSecretForeignKeys(
        hcPartyId: String,
        patientSecretForeignKeys: Set<String>
    ): Flow<String> {
        throw BridgeException()
    }

    override fun modifyEntities(entities: Flow<Contact>): Flow<Contact> = flow {
        emitAll(
            getApi()?.modifyContacts(
                entities.map(contactMapper::map).toList()
            )?.map(contactMapper::map)?.asFlow() ?: emptyFlow()
        )
    }

    override fun modifyEntities(entities: Collection<Contact>): Flow<Contact> = modifyEntities(entities.asFlow())
}
