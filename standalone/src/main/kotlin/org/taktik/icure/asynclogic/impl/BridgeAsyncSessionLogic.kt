package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.icure.cardinal.sdk.model.LoginCredentials
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.web.server.WebSession
import org.taktik.icure.asynclogic.AsyncSessionLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.constants.Roles
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.exceptions.BridgeException
import org.taktik.icure.security.BridgeCredentialsManager
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.security.KmehrJWTDetails
import org.taktik.icure.security.jwt.EncodedJWTAuth
import org.taktik.icure.security.jwt.JwtAuthentication
import org.taktik.icure.security.jwt.JwtDecoder
import org.taktik.icure.security.loadSecurityContext
import java.io.Serializable

@Service
class BridgeAsyncSessionLogic(
	private val bridgeConfig: BridgeConfig,
	private val credentialsManager: BridgeCredentialsManager,
	private val objectMapper: ObjectMapper,
	private val httpClient: HttpClient,
	private val jwtDecoder: JwtDecoder
) : AsyncSessionLogic, SessionInformationProvider {

	private val log = LoggerFactory.getLogger(this.javaClass)

	companion object {
		suspend fun getCurrentAuthentication() =
			loadSecurityContext()?.map { it.authentication as EncodedJWTAuth }?.awaitFirstOrNull()
	}

	override suspend fun getAuthentication(): Authentication {
		throw BridgeException()
	}

	override suspend fun login(
		username: String,
		password: String,
		session: WebSession?,
		groupId: String?,
		applicationId: String?,
		scopeDataOwner: String?,
		requestedSchemaVersion: Int?
	): JwtAuthentication {
		throw BridgeException()
	}

	override suspend fun logout() {
		throw BridgeException()
	}

	private suspend fun generateNewUserToken(jwt: String): String {
		val claims = jwtDecoder.decodeWithoutValidation(
			jwt = jwt
		).let { claims ->
			jwtDecoder.jwtDetailsFromClaims(KmehrJWTDetails, claims)
		}
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

	private fun isJwtExpired(jwt: String): Boolean {
		val expirationSeconds = jwtDecoder.decodeExpirationSeconds(jwt)
		return expirationSeconds < (System.currentTimeMillis() / 1000) - 30
	}

	suspend fun getCurrentJWT() =
		loadSecurityContext()?.map { (it.authentication as EncodedJWTAuth).token }
			?.awaitFirstOrNull()
			?.let { jwt ->
				val isJwtExpired = isJwtExpired(jwt)
				if(!isJwtExpired) log.debug("EXPIRED, refreshing")
				jwt.takeIf { !isJwtExpired }
					?: generateNewUserToken(jwt).also {
						val details = jwtDecoder.decodeWithoutValidation(
							jwt = jwt
						).let { claims ->
							jwtDecoder.jwtDetailsFromClaims(KmehrJWTDetails, claims)
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

	private suspend fun getKmehrSessionContext(): KmehrSessionContext =
		getCurrentAuthentication()?.let { KmehrSessionContext(it) }
			?: throw IllegalAccessException("There is no user currently logged in")

	override suspend fun getSearchKeyMatcher(): (String, HasEncryptionMetadata) -> Boolean {
		throw BridgeException()
	}

	override suspend fun getAllSearchKeysIfCurrentDataOwner(dataOwnerId: String): Set<String> {
		throw BridgeException()
	}

	override suspend fun getCallerCardinalVersion(): SemanticVersion? {
		throw BridgeException()
	}

	override suspend fun getDataOwnerAuthenticationDetails(): DataOwnerAuthenticationDetails {
		throw BridgeException()
	}

	override suspend fun getDataOwnerAuthenticationDetailsOrNull(): DataOwnerAuthenticationDetails? {
		throw BridgeException()
	}

	override suspend fun getDataOwnerHierarchyIncludingSelf(): List<String> {
		throw BridgeException()
	}

	override suspend fun getDataOwnerHierarchy(): List<String> {
		throw BridgeException()
	}

	override suspend fun getCurrentDataOwnerId(): String =
		getCurrentDataOwnerIdOrNull() ?: throw AuthenticationServiceException("Failed to extract current data owner id")

	override suspend fun getCurrentDataOwnerIdOrNull(): String? = getKmehrSessionContext().getDataOwnerId()

	override suspend fun getCurrentUserId(): String = getKmehrSessionContext().getUserId()
	override suspend fun requestsAutofixAnonymity(): Boolean {
		throw BridgeException()
	}

	private inner class KmehrSessionContext(
		private val jwt: EncodedJWTAuth
	) : SessionInformationProvider.AsyncSessionContext, Serializable {
		override fun getDataOwnerType(): DataOwnerType? = jwt.claims?.dataOwnerType

		override fun getDeviceId(): String {
			throw BridgeException()
		}

		fun getDataOwnerId(): String? = jwt.claims?.dataOwnerId

		override fun getHealthcarePartyId(): String? =
			jwt.claims?.dataOwnerId?.takeIf { jwt.claims?.dataOwnerType == DataOwnerType.HCP }

		override fun getPatientId(): String {
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
