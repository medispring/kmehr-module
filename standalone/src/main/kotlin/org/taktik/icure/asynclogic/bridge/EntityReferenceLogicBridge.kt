package org.taktik.icure.asynclogic.bridge

import com.icure.sdk.api.raw.impl.RawEntityReferenceApiImpl
import com.icure.sdk.api.raw.successBodyOrNull404
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.EntityReferenceLogic
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.asynclogic.bridge.mappers.EntityReferenceMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.errors.UnauthorizedException

@Service
class EntityReferenceLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val entityReferenceMapper: EntityReferenceMapper
) : GenericLogicBridge<EntityReference>(), EntityReferenceLogic {

    @OptIn(InternalIcureApi::class)
    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let { token ->
        RawEntityReferenceApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(token),
            httpClient = bridgeHttpClient,
            json = Serialization.json
        )
    } ?: throw UnauthorizedException("You must be logged in to perform this operation")

    @OptIn(InternalIcureApi::class)
    override suspend fun getLatest(prefix: String): EntityReference? =
        getApi().getLatest(prefix).successBodyOrNull404()?.let(entityReferenceMapper::map)

    @OptIn(InternalIcureApi::class)
    override fun createEntities(entities: Collection<EntityReference>): Flow<EntityReference> = flow {
        emitAll(
            getApi().let { api ->
                entities
                    .map { api.createEntityReference(entityReferenceMapper.map(it)).successBody() }
                    .map(entityReferenceMapper::map)
                    .asFlow()
            }
        )
    }

}
