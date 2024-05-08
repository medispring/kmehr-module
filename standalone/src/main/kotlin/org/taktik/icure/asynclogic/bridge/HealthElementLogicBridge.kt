package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.HealthElementApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.mapper.HealthElementV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class HealthElementLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val healthElementMapper: HealthElementV2Mapper
) : GenericLogicBridge<HealthElement>(), HealthElementLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        HealthElementApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }

    override fun createEntities(entities: Collection<HealthElement>): Flow<HealthElement> = flow {
        getApi()
            ?.createHealthElements(entities.map(healthElementMapper::map))
            ?.map(healthElementMapper::map)
            ?.onEach { emit(it) }
    }

    override fun deleteHealthElements(ids: Set<String>): Flow<DocIdentifier> {
        throw BridgeException()
    }

    override fun filter(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<HealthElement>
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override suspend fun getHealthElement(healthElementId: String): HealthElement? {
        throw BridgeException()
    }

    override fun getHealthElements(healthElementIds: Collection<String>): Flow<HealthElement> {
        throw BridgeException()
    }

    override suspend fun addDelegation(healthElementId: String, delegation: Delegation): HealthElement? {
        throw BridgeException()
    }

    override suspend fun addDelegations(healthElementId: String, delegations: List<Delegation>): HealthElement? {
        throw BridgeException()
    }

    override fun solveConflicts(limit: Int?): Flow<IdAndRev> {
        throw BridgeException()
    }

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<HealthElement>> {
        throw BridgeException()
    }

    override fun listHealthElementIdsByHcParty(hcpId: String): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthElementIdsByHcPartyAndCodes(
        hcPartyId: String,
        codeType: String,
        codeNumber: String
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthElementIdsByHcPartyAndSecretPatientKeys(
        hcPartyId: String,
        secretPatientKeys: List<String>
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthElementIdsByHcPartyAndStatus(hcPartyId: String, status: Int): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthElementIdsByHcPartyAndTags(
        hcPartyId: String,
        tagType: String,
        tagCode: String
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthElementsByHcPartyAndSecretPatientKeys(
        hcPartyId: String,
        secretPatientKeys: List<String>
    ): Flow<HealthElement> = flow {
        emitAll(
            getApi()?.listHealthElementsByHCPartyAndPatientForeignKeys(hcPartyId, secretPatientKeys.joinToString(","))
                ?.map(healthElementMapper::map)
                ?.asFlow() ?: emptyFlow()
        )
    }

    override fun listHealthElementIdsByDataOwnerPatientOpeningDate(
        dataOwnerId: String,
        secretForeignKeys: Set<String>,
        startDate: Long?,
        endDate: Long?,
        descending: Boolean
    ): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthElementsIdsByHcPartyAndIdentifiers(
        hcPartyId: String,
        identifiers: List<Identifier>
    ): Flow<String> {
        throw BridgeException()
    }

    override suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(
        hcPartyId: String,
        secretPatientKeys: List<String>
    ): List<HealthElement> =
        getApi()?.let{ api ->
            secretPatientKeys.fold<String, List<HealthElement>>(emptyList()) { acc, spk ->
                acc + api.listHealthElementsByHCPartyAndPatientForeignKeys(hcPartyId, spk)
                    .map { healthElementMapper.map(it) }
            }.groupBy {
                it.healthElementId
            }.values.mapNotNull { value ->
                value.maxByOrNull { it.modified ?: it.created ?: 0L }
            }
        } ?: emptyList()

    override suspend fun modifyHealthElement(healthElement: HealthElement): HealthElement? {
        throw BridgeException()
    }
}
