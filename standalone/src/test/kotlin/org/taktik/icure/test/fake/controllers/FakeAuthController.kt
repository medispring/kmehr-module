package org.taktik.icure.test.fake.controllers

import io.icure.kraken.client.apis.UserApi
import io.icure.kraken.client.security.ExternalJWTProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.security.jwt.EncodedJWTAuth
import org.taktik.icure.security.loadSecurityContext

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
@RestController
@RequestMapping("/rest/test/auth")
class FakeAuthController(
    private val bridgeConfig: BridgeConfig
) {

    @GetMapping("/currentuser")
    fun getCurrentUser() = mono {
        val currentToken = loadSecurityContext()?.map { (it.authentication as EncodedJWTAuth).token }?.awaitFirstOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val userApi = UserApi(basePath = bridgeConfig.iCureUrl, authProvider = ExternalJWTProvider(currentToken))
        userApi.getCurrentUser()
    }

}
