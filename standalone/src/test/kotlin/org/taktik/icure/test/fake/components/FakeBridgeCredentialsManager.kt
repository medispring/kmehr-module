package org.taktik.icure.test.fake.components

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
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.security.BridgeCredentialsManager
import org.taktik.icure.test.testHttpClient

@OptIn(InternalIcureApi::class)
class FakeBridgeCredentialsManager(
    bridgeConfig: BridgeConfig,
    kmehrUsername: String,
    kmehrPwd: String
) : BridgeCredentialsManager {

    private val provider: JwtBasedAuthProvider = AuthenticationMethod.UsingCredentials(
        UsernamePassword(
            username = kmehrUsername,
            password = kmehrPwd
        )
    ).getAuthProvider(
        authApi = RawAnonymousAuthApiImpl(bridgeConfig.iCureUrl, testHttpClient, json = Serialization.json),
        cryptoService = defaultCryptoService,
        applicationId = null,
        options = BasicApiOptions(),
        messageGatewayApi = RawMessageGatewayApi(testHttpClient)
    ) as JwtBasedAuthProvider

    override suspend fun getModuleJwt() = provider.getBearerAndRefreshToken().bearer.token
}