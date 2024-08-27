package org.taktik.icure.test.fake.controllers

import com.icure.sdk.api.raw.impl.RawUserApiImpl
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.asynclogic.bridge.auth.KmehrAuthProvider
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.base.Code
import org.taktik.icure.security.jwt.EncodedJWTAuth
import org.taktik.icure.security.loadSecurityContext
import org.taktik.icure.test.testHttpClient

@RestController
@RequestMapping("/rest/test")
class FakeController(
    private val bridgeConfig: BridgeConfig
) {

    @OptIn(InternalIcureApi::class)
    @GetMapping("/auth/currentuser")
    fun getCurrentUser() = mono {
        val currentToken = loadSecurityContext()?.map { (it.authentication as EncodedJWTAuth).token }?.awaitFirstOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val userApi = RawUserApiImpl(
            apiUrl = bridgeConfig.iCureUrl,
            authProvider = KmehrAuthProvider(currentToken),
            httpClient = testHttpClient,
            json = Serialization.json
        )
        userApi.getCurrentUser().successBody()
    }

    @PostMapping("/big")
    fun receiveBigPayload(@RequestBody payload: Code) = payload

}
