package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.web.server.WebSession
import org.taktik.icure.asynclogic.AsyncSessionLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.constants.Roles
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.Encryptable
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.exceptions.ForbiddenRequestException
import org.taktik.icure.security.BridgeCredentialsManager
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.security.KmehrJWTDetails
import org.taktik.icure.security.jwt.EncodedJWTAuth
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.security.loadSecurityContext
import org.taktik.icure.services.external.rest.v2.dto.LoginCredentials
import java.io.Serializable

@Service
class BridgeAsyncSessionLogic(
    private val bridgeConfig: BridgeConfig,
    private val credentialsManager: BridgeCredentialsManager,
    private val jwtUtils: JwtUtils,
    private val objectMapper: ObjectMapper
) : AsyncSessionLogic, SessionInformationProvider {

    private val log = LoggerFactory.getLogger(this.javaClass)

    companion object {
        private suspend fun getCurrentAuthentication() =
            loadSecurityContext()?.map { it.authentication as EncodedJWTAuth }?.awaitFirstOrNull()
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun getAuthentication(): Authentication {
        throw BridgeException()
    }

    override suspend fun login(
        username: String,
        password: String,
        session: WebSession?,
        groupId: String?
    ): Authentication? {
        throw BridgeException()
    }

    override suspend fun logout() {
        throw BridgeException()
    }

    private suspend fun generateNewUserToken(jwt: String): String {
        val claims = jwtUtils.jwtDetailsFromClaims(KmehrJWTDetails, jwtUtils.decodeAndGetClaims(jwt, true))
        val tmpToken = httpClient.post(
            "${bridgeConfig.iCureUrl}/rest/v2/user/inGroup/${claims.groupId}/token/${claims.userId}/kmehr?tokenValidity=60"
        ) {
            header("Authorization", "Bearer ${credentialsManager.getModuleJwt()}")
        }.bodyAsText()
        return httpClient.post("${bridgeConfig.iCureUrl}/rest/v2/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(LoginCredentials(
                username = "${claims.groupId}/${claims.userId}",
                password = tmpToken
            )))
        }.let { objectMapper.readValue<JwtResponse>(it.bodyAsText()) }.token
    }

    suspend fun getCurrentJWT() =
        loadSecurityContext()?.map { (it.authentication as EncodedJWTAuth).token }
            ?.awaitFirstOrNull()
            ?.let{ jwt ->
                if(!jwtUtils.isNotExpired(jwt)) log.debug("EXPIRED, refreshing")
                jwt.takeIf { jwtUtils.isNotExpired(it) }
                    ?: generateNewUserToken(jwt).also {
                        val details = jwtUtils.decodeAndGetClaims(it).let { claims ->
                            jwtUtils.jwtDetailsFromClaims(KmehrJWTDetails, claims)
                        }
                        loadSecurityContext()?.map { ctx ->
                            ctx.authentication = EncodedJWTAuth(
                                token = it,
                                claims = details,
                                authorities = mutableSetOf(SimpleGrantedAuthority(Roles.GrantedAuthority.ROLE_USER))
                            )
                        }?.awaitFirstOrNull()
                    }
            }

    override suspend fun getCurrentSessionContext(): SessionInformationProvider.AsyncSessionContext =
        getCurrentAuthentication()?.let { KmehrSessionContext(it) }
            ?: throw IllegalAccessException("There is no user currently logged in")

    override suspend fun getCurrentHealthcarePartyId(): String =
        getCurrentSessionContext().getHealthcarePartyId()
            ?: throw ForbiddenRequestException("Current user is not an Healthcare Party")

    override suspend fun getSearchKeyMatcher(): (String, HasEncryptionMetadata) -> Boolean {
        throw BridgeException()
    }

    override suspend fun getDataOwnerAuthenticationDetails(): DataOwnerAuthenticationDetails {
        throw BridgeException()
    }

    override suspend fun getCurrentDataOwnerId(): String {
        throw BridgeException()
    }

    override suspend fun getCurrentUserId(): String = getCurrentSessionContext().getUserId()
    override suspend fun requestsAutofixAnonymity(): Boolean {
        throw BridgeException()
    }

    private inner class KmehrSessionContext(
        private val jwt: EncodedJWTAuth
    ) : SessionInformationProvider.AsyncSessionContext, Serializable {
        override fun getDataOwnerType(): DataOwnerType? = jwt.claims?.dataOwnerType

        override fun getDeviceId(): String? {
            throw BridgeException()
        }

        override fun getHcpHierarchy(): List<String> {
            throw BridgeException()
        }

        override fun getHealthcarePartyId(): String? =
            jwt.claims?.dataOwnerId?.takeIf { jwt.claims?.dataOwnerType == DataOwnerType.HCP }

        override fun getPatientId(): String? {
            throw BridgeException()
        }

        override fun getGlobalUserId(): String {
            throw BridgeException()
        }
        override fun getUserId(): String = requireNotNull(jwt.claims?.userId) { "JWT does not contain a valid userId"}

    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class JwtResponse(
    val token: String
)
