package org.taktik.icure.asynclogic.bridge

import io.icure.kraken.client.apis.EntityrefApi
import io.icure.kraken.client.infrastructure.ClientException
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.EntityReferenceLogic
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.entities.utils.ExternalFilterKey
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.services.external.rest.v2.mapper.EntityReferenceV2Mapper

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Service
class EntityReferenceLogicBridge(
    private val asyncSessionLogic: BridgeAsyncSessionLogic,
    private val bridgeConfig: BridgeConfig,
    private val entityReferenceMapper: EntityReferenceV2Mapper
) : GenericLogicBridge<EntityReference>(), EntityReferenceLogic {

    private suspend fun getApi() = asyncSessionLogic.getCurrentJWT()?.let {
        EntityrefApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(it))
    }

    override suspend fun getLatest(prefix: String): EntityReference? =
        getApi()?.getLatest(prefix)?.let(entityReferenceMapper::map)

    override fun createEntities(entities: Collection<EntityReference>): Flow<EntityReference> = flow {
        emitAll(
            getApi()?.let { api ->
                entities
                    .map { api.createEntityReference(entityReferenceMapper.map(it)) }
                    .map(entityReferenceMapper::map)
                    .asFlow()
            } ?: emptyFlow()
        )
    }

}
