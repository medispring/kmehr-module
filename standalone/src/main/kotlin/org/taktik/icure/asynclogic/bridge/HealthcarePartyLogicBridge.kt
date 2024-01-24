package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.HealthcarePartyApi
import io.icure.kraken.client.infrastructure.ClientException
import io.icure.kraken.client.security.ExternalJWTProvider
import io.jsonwebtoken.JwtException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.HealthcarePartyLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.ListOfIdsDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthcarePartyV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class HealthcarePartyLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val healthcarePartyMapper: HealthcarePartyV2Mapper
) : GenericLogicBridge<HealthcareParty>(), HealthcarePartyLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        HealthcarePartyApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(token))
    }


    override suspend fun createHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty? =
        getApi()?.createHealthcareParty(healthcarePartyMapper.map(healthcareParty))
            ?.let { healthcarePartyMapper.map(it) }

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
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findHealthcarePartiesBy(offset: PaginationOffset<String>, desc: Boolean?): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override fun findHealthcarePartiesBySsinOrNihii(
        searchValue: String,
        paginationOffset: PaginationOffset<String>,
        desc: Boolean,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    override suspend fun getAesExchangeKeysForDelegate(healthcarePartyId: String): Map<String, Map<String, Map<String, String>>> {
        throw BridgeException()
    }

    @Deprecated("A HCP may now have multiple AES Keys. Use getAesExchangeKeysForDelegate instead")
    override suspend fun getHcPartyKeysForDelegate(healthcarePartyId: String): Map<String, String> {
        throw BridgeException()
    }

    private suspend fun getHcpHierarchyIdsRecursive(
        api: HealthcarePartyApi,
        healthcarePartyId: String?,
        hcpHierarchy: Set<String> = emptySet()
    ): Set<String> =
        if (healthcarePartyId != null) {
            try {
                val hcp = api.getHealthcareParty(healthcarePartyId)
                getHcpHierarchyIdsRecursive(api, hcp.parentId, hcpHierarchy + healthcarePartyId)
            } catch (e: ClientException) {
                hcpHierarchy
            }
        } else hcpHierarchy


    override suspend fun getHcpHierarchyIds(sender: HealthcareParty): HashSet<String> =
        getApi()?.let { getHcpHierarchyIdsRecursive(it, sender.id).toHashSet() } ?: HashSet()

    override fun getHealthcareParties(ids: List<String>): Flow<HealthcareParty> = flow {
        emitAll(
            getApi()
                ?.getHealthcareParties(ListOfIdsDto( ids = ids ))
                ?.map { healthcarePartyMapper.map(it)  }
                ?.asFlow() ?: emptyFlow()
        )
    }

    override fun getHealthcarePartiesByParentId(parentId: String): Flow<HealthcareParty> {
        throw BridgeException()
    }

    override suspend fun getHealthcareParty(id: String): HealthcareParty? =
        getApi()?.getHealthcareParty(id)?.let {
            healthcarePartyMapper.map(it)
        }

    override suspend fun getPublicKey(healthcarePartyId: String): String? {
        throw BridgeException()
    }

    override fun listHealthcarePartiesBy(searchString: String, offset: Int, limit: Int): Flow<HealthcareParty> {
        throw BridgeException()
    }

    private fun findHealthcarePartiesByNameRecursive(
        name: String,
        desc: Boolean? = null,
        startKey: String? = null,
        startDocumentId: String? = null
    ) : Flow<HealthcarePartyDto> = flow {
        val result = getApi()?.findHealthcarePartiesByName(
            name = name,
            startKey = startKey,
            startDocumentId = startDocumentId,
            limit = 1000,
            desc = desc
        ) ?: throw JwtException("Invalid JWT")
        emitAll(result.rows.asFlow())
        if(result.nextKeyPair?.startKeyDocId != null) {
            emitAll(
                findHealthcarePartiesByNameRecursive(
                    name,
                    desc,
                    result.nextKeyPair?.startKey as? String,
                    result.nextKeyPair?.startKeyDocId
                )
            )
        }
    }

    override fun listHealthcarePartiesByName(name: String): Flow<HealthcareParty> = flow {
        emitAll(
            findHealthcarePartiesByNameRecursive(name).map(healthcarePartyMapper::map)
        )
    }

    override fun listHealthcarePartiesBySpecialityAndPostcode(
        type: String,
        spec: String,
        firstCode: String,
        lastCode: String,
    ): Flow<ViewQueryResultEvent> {
        throw BridgeException()
    }

    private fun findHealthcarePartiesBySsinOrNihiiRecursive(
        query: String,
        startKey: String? = null,
        startDocumentId: String? = null
    ) : Flow<HealthcarePartyDto> = flow {
        val result = getApi()?.findHealthcarePartiesBySsinOrNihii(
            searchValue = query,
            startKey = startKey,
            startDocumentId = startDocumentId,
            limit = 1000,
            desc = false
        ) ?: throw JwtException("Invalid JWT")
        emitAll(result.rows.asFlow())
        if(result.nextKeyPair?.startKeyDocId != null) {
            emitAll(
                findHealthcarePartiesBySsinOrNihiiRecursive(
                    query,
                    result.nextKeyPair?.startKey as? String,
                    result.nextKeyPair?.startKeyDocId
                )
            )
        }
    }

    override fun listHealthcarePartiesBySsin(ssin: String): Flow<HealthcareParty> = flow {
        emitAll(
            findHealthcarePartiesBySsinOrNihiiRecursive(ssin)
                .filter { it.ssin == ssin }
                .map(healthcarePartyMapper::map)
        )
    }

    override fun listHealthcarePartiesByNihii(nihii: String): Flow<HealthcareParty> = flow {
        emitAll(
            findHealthcarePartiesBySsinOrNihiiRecursive(nihii)
                .filter { it.nihii == nihii }
                .map(healthcarePartyMapper::map)
        )
    }

    override fun listHealthcarePartyIdsByCode(codeType: String, codeCode: String?): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthcarePartyIdsByIdentifiers(hcpIdentifiers: List<Identifier>): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthcarePartyIdsByName(name: String, desc: Boolean): Flow<String> {
        throw BridgeException()
    }

    override fun listHealthcarePartyIdsByTag(tagType: String, tagCode: String?): Flow<String> {
        throw BridgeException()
    }

    override suspend fun modifyHealthcareParty(healthcareParty: HealthcareParty): HealthcareParty? {
        throw BridgeException()
    }

}
