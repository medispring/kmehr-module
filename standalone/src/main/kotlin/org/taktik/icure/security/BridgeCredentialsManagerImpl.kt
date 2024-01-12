package org.taktik.icure.security

import io.icure.kraken.client.security.JWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component
import org.taktik.icure.config.BridgeConfig

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@Component
class BridgeCredentialsManagerImpl(
    bridgeConfig: BridgeConfig
) : BridgeCredentialsManager {

    private val provider = JWTProvider(bridgeConfig.iCureUrl, bridgeConfig.kmehrUsername, bridgeConfig.kmehrPwd).also {
        println(bridgeConfig)
    }

    override suspend fun getModuleJwt() = provider.getAuthHeader().replace("Bearer ", "")

}
