package org.taktik.icure.test.fake.components

import io.icure.kraken.client.apis.AuthApi
import io.icure.kraken.client.models.LoginCredentials
import io.icure.kraken.client.security.AuthProvider
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.DefaultClaims
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.security.BridgeCredentialsManager
import java.util.*

class FakeBridgeCredentialsManager(
    bridgeConfig: BridgeConfig,
    kmehrUsername: String,
    kmehrPwd: String
) : BridgeCredentialsManager {

    private val provider = FakeJWTProvider(bridgeConfig.iCureUrl, kmehrUsername, kmehrPwd, 1_000)

    override suspend fun getModuleJwt()  = provider.getAuthHeader().replace("Bearer ", "")
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class FakeJWTProvider(
    basePath: String,
    private val username: String,
    private val password: String,
    private val refreshPadding: Long = 30_000
) : AuthProvider {

    private val mutex = Mutex()
    private val authApi = AuthApi(basePath)
    private var authJWT: String? = null
    private var refreshJWT: String? = null

    override suspend fun getAuthHeader(): String =
        mutex.withLock {
            println(authJWT)
            println(authJWT == null || isJwtExpired(authJWT!!))
            if (authJWT == null || isJwtExpired(authJWT!!)) {
                refreshAuthenticationJWT()
            }
            println(authJWT)
        }.let { "Bearer ${authJWT!!}" }

    private suspend fun refreshAuthenticationJWT() =
        if (refreshJWT == null || isJwtExpired(refreshJWT!!)) {
            println("LOGIN")
            authApi.login(LoginCredentials(username, password))
        } else {
            println("REFRESH")
            authApi.refresh(refreshJWT!!)
        }.let {
            authJWT = it.token!!
            refreshJWT = it.refreshToken ?: refreshJWT
        }

    private fun isJwtExpired(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) return true
        return try {
            val expiration =
                (Jwts.parserBuilder().build().parse("${parts[0]}.${parts[1]}.").body as DefaultClaims).expiration
            expiration < Date(System.currentTimeMillis() + refreshPadding)
        } catch (e: Exception) {
            true
        }
    }

}
