package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawHealthElementApiImpl
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.HealthElementMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException

@Service
class HealthElementLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val healthElementMapper: HealthElementMapper
) : GenericLogicBridge<HealthElement>(), HealthElementLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawHealthElementApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json,
            accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    @OptIn(InternalIcureApi::class)
    override fun createEntities(entities: Collection<HealthElement>): Flow<HealthElement> = flow {
        emitAll(getApi()
            .createHealthElements(entities.map(healthElementMapper::map))
            .successBody()
            .map(healthElementMapper::map)
            .asFlow()
        )
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

    override fun bulkShareOrUpdateMetadata(requests: BulkShareOrUpdateMetadataParams): Flow<EntityBulkShareResult<HealthElement>> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override fun listHealthElementsByHcPartyAndSecretPatientKeys(
        hcPartyId: String,
        secretPatientKeys: List<String>
    ): Flow<HealthElement> = flow {
        emitAll(
            getApi().listHealthElementsByHCPartyAndPatientForeignKeys(hcPartyId, secretPatientKeys.joinToString(","))
                .successBody()
                .map(healthElementMapper::map)
                .asFlow()
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

    @OptIn(InternalIcureApi::class)
    override suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(
        hcPartyId: String,
        secretPatientKeys: List<String>
    ): List<HealthElement> = getApi().let{ api ->
        secretPatientKeys.fold<String, List<HealthElement>>(emptyList()) { acc, spk ->
            acc + api.listHealthElementsByHCPartyAndPatientForeignKeys(hcPartyId, spk)
                .successBody()
                .map(healthElementMapper::map)
        }.groupBy {
            it.healthElementId
        }.values.mapNotNull { value ->
            value.maxByOrNull { it.modified ?: it.created ?: 0L }
        }
    }

    override suspend fun modifyHealthElement(healthElement: HealthElement): HealthElement? {
        throw BridgeException()
    }
}
