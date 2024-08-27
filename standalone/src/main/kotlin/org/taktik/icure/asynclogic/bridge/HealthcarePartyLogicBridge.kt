package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.RawHealthcarePartyApi
import com.icure.sdk.api.raw.impl.RawHealthcarePartyApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.model.ListOfIds
import com.icure.sdk.model.filter.hcparty.HealthcarePartyByNationalIdentifierFilter
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.RequestStatusException
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import com.icure.sdk.model.HealthcareParty as SdkHealthcareParty
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.HealthcarePartyMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.errors.UnauthorizedException
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.pagination.PaginationElement

@Service
class HealthcarePartyLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val healthcarePartyMapper: HealthcarePartyMapper
) : GenericLogicBridge<HealthcareParty>(), HealthcarePartyLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawHealthcarePartyApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")


    @OptIn(InternalIcureApi::class)
    override suspend fun createHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty? =
        getApi().createHealthcareParty(healthcarePartyMapper.map(healthcareParty))
            .successBody()
            .let { healthcarePartyMapper.map(it) }

    override fun deleteHealthcareParties(healthcarePartyIds: List<String>): Flow<DocIdentifier> {
        throw BridgeException()
    }

    override fun filterHealthcareParties(
        paginationOffset: PaginationOffset<Nothing>,
        filter: FilterChain<HealthcareParty>,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findHealthcarePartiesBy(
        fuzzyName: String,
        offset: PaginationOffset<String>,
        desc: Boolean?,
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun findHealthcarePartiesBy(offset: PaginationOffset<String>, desc: Boolean?): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun findHealthcarePartiesBySsinOrNihii(
        searchValue: String,
        paginationOffset: PaginationOffset<String>,
        desc: Boolean,
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
        throw BridgeException()
    }

    @Deprecated("A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
    override suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    private suspend fun getHcpHierarchyIdsRecursive(
        api: RawHealthcarePartyApi,
        healthcarePartyId: String?,
        hcpHierarchy: Set<String> = emptySet()
    ): Set<String> =
        if (healthcarePartyId != null) {
            try {
                val hcp = api.getHealthcareParty(healthcarePartyId).successBody()
                getHcpHierarchyIdsRecursive(api, hcp.parentId, hcpHierarchy + healthcarePartyId)
            } catch (e: RequestStatusException) {
                hcpHierarchy
            }
        } else hcpHierarchy


    @OptIn(InternalIcureApi::class)
    override suspend fun getHcpHierarchyIds(sender: HealthcareParty): HashSet<String> =
        getApi().let { getHcpHierarchyIdsRecursive(it, sender.id).toHashSet() }

    @OptIn(InternalIcureApi::class)
    override fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty> =
        if(ids.isNotEmpty()) flow {
            emitAll(
                getApi()
                    .getHealthcareParties(ListOfIds( ids = ids ))
                    .successBody()
                    .map(healthcarePartyMapper::map)
                    .asFlow()
            )
        } else emptyFlow()

    override fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    override suspend fun getHealthcareParty(id: String): HealthcareParty? =
        getApi().getHealthcareParty(id).successBody().let {
            healthcarePartyMapper.map(it)
        }

    override suspend fun getPublicKey(healthcarePartyId: String): String? {
        throw BridgeException()
    }

    override fun listHealthcarePartiesBySpecialityAndPostcode(
        type: String,
        spec: String,
        firstCode: String,
        lastCode: String,
        offset: PaginationOffset<ComplexKey>
    ): Flow<PaginationElement> {
        throw BridgeException()
    }

    override fun listHealthcarePartiesBy(searchString: String, offset: Int, limit: Int): Flow<HealthcareParty> {
        throw BridgeException()
    }

    @OptIn(InternalIcureApi::class)
    private fun findHealthcarePartiesByNameRecursive(
        name: String,
        desc: Boolean? = null,
        startKey: JsonElement? = null,
        startDocumentId: String? = null
    ) : Flow<HealthcareParty> = flow {
        val result = getApi().findHealthcarePartiesByName(
            name = name,
            startKey = startKey?.let { Serialization.json.encodeToString(it) },
            startDocumentId = startDocumentId,
            limit = 1000,
            desc = desc
        ).successBody()
        emitAll(result.rows.map(healthcarePartyMapper::map).asFlow())
        if(result.nextKeyPair?.startKeyDocId != null) {
            emitAll(
                findHealthcarePartiesByNameRecursive(
                    name,
                    desc,
                    result.nextKeyPair?.startKey,
                    result.nextKeyPair?.startKeyDocId
                )
            )
        }
    }

    override fun listHealthcarePartiesByName(name: String): Flow<HealthcareParty> = flow {
        emitAll(findHealthcarePartiesByNameRecursive(name))
    }

    // Note: the batch size is intentionally small because in all the usages of this function only the first
    // result is taken, so there is no need to have a bigger batch size.
    @OptIn(InternalIcureApi::class)
    private fun listHealthcarePartiesBySsinOrNihii(query: String, filter: (SdkHealthcareParty) -> Boolean): Flow<HealthcareParty> = flow {
        val api = getApi()
        val hcpIds = api.matchHealthcarePartiesBy(HealthcarePartyByNationalIdentifierFilter(query)).successBody()

        hcpIds.chunked(50).forEach { batch ->
            emitAll(api
                .getHealthcareParties(ListOfIds(batch))
                .successBody()
                .filter(filter)
                .map(healthcarePartyMapper::map)
                .asFlow()
            )
        }
    }


    override fun listHealthcarePartiesBySsin(ssin: String): Flow<HealthcareParty> =
        listHealthcarePartiesBySsinOrNihii(ssin) { it.ssin == ssin }

    // Note: the batch size is intentionally small because in all the usages of this function only the first
    // result is taken, so there is no need to have a bigger batch size.
    @OptIn(InternalIcureApi::class)
    override fun listHealthcarePartiesByNihii(nihii: String): Flow<HealthcareParty> =
        listHealthcarePartiesBySsinOrNihii(nihii) { it.nihii == nihii }

    override suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty? {
        throw BridgeException()
    }

}
