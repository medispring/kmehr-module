package org.taktik.icure.security

import com.icure.kryptom.crypto.defaultCryptoService
import com.icure.sdk.api.raw.RawMessageGatewayApi
import com.icure.sdk.api.raw.impl.RawAnonymousAuthApiImpl
import com.icure.sdk.auth.UsernamePassword
import com.icure.sdk.auth.services.JwtBasedAuthProvider
import com.icure.sdk.options.AuthenticationMethod
import com.icure.sdk.options.BasicApiOptions
import com.icure.sdk.options.getAuthProvider
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.springframework.stereotype.Component
import org.taktik.icure.config.BridgeConfig

@OptIn(InternalIcureApi::class)
@Component
class BridgeCredentialsManagerImpl(
    bridgeConfig: BridgeConfig
) : BridgeCredentialsManager {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json = Serialization.json)
        }
    }

    private val provider: JwtBasedAuthProvider = AuthenticationMethod.UsingCredentials(
        UsernamePassword(
            username = bridgeConfig.kmehrUsername,
            password = bridgeConfig.kmehrPwd
        )
    ).getAuthProvider(
        authApi = RawAnonymousAuthApiImpl(bridgeConfig.iCureUrl, httpClient, json = Serialization.json),
        cryptoService = defaultCryptoService,
        applicationId = null,
        options = BasicApiOptions(),
        messageGatewayApi = RawMessageGatewayApi(httpClient)
    ) as JwtBasedAuthProvider

    override suspend fun getModuleJwt() = provider.getBearerAndRefreshToken().bearer.token

}
