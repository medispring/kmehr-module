package org.taktik.icure.test

import com.icure.kryptom.crypto.defaultCryptoService
import com.icure.sdk.api.raw.RawMessageGatewayApi
import com.icure.sdk.api.raw.impl.RawAnonymousAuthApiImpl
import com.icure.sdk.api.raw.impl.RawHealthcarePartyApiImpl
import com.icure.sdk.api.raw.impl.RawPatientApiImpl
import com.icure.sdk.api.raw.impl.RawPermissionApiImpl
import com.icure.sdk.api.raw.impl.RawUserApiImpl
import com.icure.sdk.auth.UsernamePassword
import com.icure.sdk.auth.services.JwtBasedAuthProvider
import com.icure.sdk.crypto.impl.NoAccessControlKeysHeadersProvider
import com.icure.sdk.model.EncryptedPatient
import com.icure.sdk.model.HealthcareParty
import com.icure.sdk.model.User
import com.icure.sdk.model.security.AlwaysPermissionItem
import com.icure.sdk.model.security.Permission
import com.icure.sdk.model.security.PermissionType
import com.icure.sdk.options.AuthenticationMethod
import com.icure.sdk.options.BasicApiOptions
import com.icure.sdk.options.getAuthProvider
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.taktik.icure.constants.Roles
import org.taktik.icure.security.KmehrJWTDetails
import org.taktik.icure.security.jwt.EncodedJWTAuth
import org.taktik.icure.security.jwt.JwtDetails
import org.taktik.icure.security.jwt.JwtUtils
import reactor.core.publisher.Mono
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

fun uuid() = UUID.randomUUID().toString()
fun ssin() = "${Random.nextInt(10,99)}.${Random.nextInt(10,12)}.${Random.nextInt(10,28)}-${Random.nextInt(100,999)}.${Random.nextInt(10,99)}"
fun generateEmail() = "${uuid().subSequence(0, 6)}@icure.test"

val testHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(json = Serialization.json)
    }
}

@OptIn(InternalIcureApi::class)
val authProviders = mutableMapOf<Pair<String, String>, JwtBasedAuthProvider>()

@OptIn(InternalIcureApi::class)
fun getAuthProvider(iCureUrl: String, username: String, password: String) =
    authProviders[Pair(username, password)] ?: AuthenticationMethod.UsingCredentials(
        UsernamePassword(username, password)
    ).getAuthProvider(
        authApi = RawAnonymousAuthApiImpl(iCureUrl, testHttpClient, json = Serialization.json),
        cryptoService = defaultCryptoService,
        applicationId = null,
        options = BasicApiOptions(),
        messageGatewayApi = RawMessageGatewayApi(testHttpClient)
    ) as JwtBasedAuthProvider

@OptIn(InternalIcureApi::class)
suspend fun getAuthJWT(iCureUrl: String, username: String, password: String) =
    getAuthProvider(iCureUrl, username, password).getBearerAndRefreshToken().bearer.token

data class UserCredentials(
    val userId: String,
    val login: String,
    val password: String,
    val dataOwnerId: String? = null,
    val authJWT: String? = null,
    val jwtClaims: JwtDetails? = null
)

@OptIn(InternalIcureApi::class)
suspend fun createHealthcarePartyUser(iCureUrl: String, username: String, password: String, jwtUtils: JwtUtils): UserCredentials {
    val login = generateEmail()
    val authProvider = getAuthProvider(iCureUrl, username, password)

    val hcp = RawHealthcarePartyApiImpl(
        apiUrl = iCureUrl,
        authProvider = authProvider,
        httpClient = testHttpClient,
        json = Serialization.json
    ).createHealthcareParty(
        HealthcareParty(
            id = uuid(),
            firstName = "hcp",
            lastName = login,
        )
    ).successBody()

    val userPassword = uuid()
    val user = RawUserApiImpl(
        apiUrl = iCureUrl,
        authProvider = authProvider,
        httpClient = testHttpClient,
        json = Serialization.json
    ).createUser(
        User(
            id = uuid(),
            login = login,
            email = login,
            passwordHash = userPassword,
            healthcarePartyId = hcp.id
        )
    ).successBody()

    val authJwt = getAuthJWT(iCureUrl, login, userPassword)

    return UserCredentials(
        user.id,
        login,
        userPassword,
        hcp.id,
        authJwt,
        jwtUtils.decodeAndGetClaims(authJwt).let { jwtUtils.jwtDetailsFromClaims(KmehrJWTDetails, it) }
    )
}

@OptIn(InternalIcureApi::class)
suspend fun createPatientUser(iCureUrl: String, username: String, password: String): UserCredentials {
    val login = generateEmail()
    val authProvider = getAuthProvider(iCureUrl, username, password)

    val patient = RawPatientApiImpl(
        apiUrl = iCureUrl,
        authProvider = authProvider,
        httpClient = testHttpClient,
        json = Serialization.json,
        accessControlKeysHeadersProvider = NoAccessControlKeysHeadersProvider
    ).createPatient(
        EncryptedPatient(
            id = uuid(),
            firstName = "patient",
            lastName = login,
        )
    ).successBody()

    val userPassword = uuid()
    val user = RawUserApiImpl(
        apiUrl = iCureUrl,
        authProvider = authProvider,
        httpClient = testHttpClient,
        json = Serialization.json
    ).createUser(
        User(
            id = uuid(),
            login = login,
            email = login,
            passwordHash = userPassword,
            patientId = patient.id
        )
    ).successBody()

    val authJwt = getAuthJWT(iCureUrl, login, userPassword)

    return UserCredentials(
        user.id,
        login,
        userPassword,
        patient.id,
        authJwt
    )
}

@OptIn(InternalIcureApi::class)
suspend fun createAdminUser(iCureUrl: String, username: String, password: String): UserCredentials {
    val login = generateEmail()
    val authProvider = getAuthProvider(iCureUrl, username, password)

    val userPassword = uuid()
    val user = RawUserApiImpl(
        apiUrl = iCureUrl,
        authProvider = authProvider,
        httpClient = testHttpClient,
        json = Serialization.json
    ).createUser(
        User(
            id = uuid(),
            login = login,
            email = login,
            passwordHash = userPassword
        )
    ).successBody()

    RawPermissionApiImpl(
        apiUrl = iCureUrl,
        authProvider = authProvider,
        httpClient = testHttpClient,
        json = Serialization.json
    ).modifyUserPermissions(
        "${user.groupId}:${user.id}",
        Permission(
            grants = setOf(
                AlwaysPermissionItem(
                    PermissionType.Admin
                )
            )
        )
    ).successBody()

    val authJwt = getAuthJWT(iCureUrl, login, userPassword)

    return UserCredentials(
        user.id,
        login,
        userPassword,
        null,
        authJwt
    )
}

suspend fun <T> withAuthenticatedReactorContext(credentials: UserCredentials, block: suspend CoroutineScope.() -> T): T {
    val fakeSecurityContext = object : SecurityContext {

        var auth: Authentication = EncodedJWTAuth(
            token = credentials.authJWT.shouldNotBeNull(),
            claims = credentials.jwtClaims.shouldNotBeNull(),
            authorities = mutableSetOf(
                SimpleGrantedAuthority(Roles.GrantedAuthority.ROLE_USER),
                SimpleGrantedAuthority(Roles.GrantedAuthority.ROLE_HCP)
            )
        )

        override fun getAuthentication(): Authentication = auth

        override fun setAuthentication(authentication: Authentication?) {
            authentication?.let {
                auth = it
            }
        }
    }

    val ctx = ReactiveSecurityContextHolder.withSecurityContext(Mono.just(fakeSecurityContext))
    return withContext(coroutineContext.plus(ctx?.asCoroutineContext() as CoroutineContext), block)
}

fun List<DataBuffer>.combineToString(): String {
    val dataBufferFactory = DefaultDataBufferFactory()
    val combinedBuffer = dataBufferFactory.join(this)
    val byteArray = ByteArray(combinedBuffer.readableByteCount())
    combinedBuffer.read(byteArray)
    return String(byteArray, Charsets.UTF_8)
}