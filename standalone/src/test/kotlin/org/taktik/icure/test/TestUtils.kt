package org.taktik.icure.test

import io.icure.kraken.client.apis.AuthApi
import io.icure.kraken.client.apis.HealthcarePartyApi
import io.icure.kraken.client.apis.PatientApi
import io.icure.kraken.client.apis.PermissionApi
import io.icure.kraken.client.apis.UserApi
import io.icure.kraken.client.crypto.toPrivateKey
import io.icure.kraken.client.crypto.toPublicKey
import io.icure.kraken.client.models.LoginCredentials
import io.icure.kraken.client.security.JWTProvider
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.security.AlwaysPermissionItemDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionTypeDto
import reactor.core.publisher.Mono
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

fun uuid() = UUID.randomUUID().toString()

fun ssin() = "${Random.nextInt(10,99)}.${Random.nextInt(10,12)}.${Random.nextInt(10,28)}-${Random.nextInt(100,999)}.${Random.nextInt(10,99)}"

fun generateEmail() = "${uuid().subSequence(0, 6)}@icure.test"

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
suspend fun getAuthJWT(iCureUrl: String, username: String, password: String) =
    AuthApi(basePath = iCureUrl)
        .login(LoginCredentials(username, password))
        .token!!

data class UserCredentials(
    val userId: String,
    val login: String,
    val password: String,
    val dataOwnerId: String? = null,
    val privateKey: RSAPrivateKey? = null,
    val publicKey: RSAPublicKey? = null,
    val authJWT: String? = null,
    val jwtClaims: JwtDetails? = null
)

@OptIn(ExperimentalUnsignedTypes::class)
private fun ByteArray.keyToHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }


@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
suspend fun createHealthcarePartyUser(iCureUrl: String, username: String, password: String, jwtUtils: JwtUtils): UserCredentials {
    val login = generateEmail()
    val rsaKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
    val pubKey = rsaKeyPair.public.encoded.keyToHexString()

    val hcp = HealthcarePartyApi(basePath = iCureUrl, authProvider = JWTProvider(iCureUrl, username, password))
        .createHealthcareParty(
            HealthcarePartyDto(
                id = uuid(),
                firstName = "hcp",
                lastName = login,
                publicKey = pubKey
            )
        )

    val userPassword = uuid()
    val user = UserApi(basePath = iCureUrl, authProvider = JWTProvider(iCureUrl, username, password))
        .createUser(
            UserDto(
                id = uuid(),
                login = login,
                email = login,
                passwordHash = userPassword,
                healthcarePartyId = hcp.id
            )
        )

    val authJwt = getAuthJWT(iCureUrl, login, userPassword)

    return UserCredentials(
        user.id,
        login,
        userPassword,
        hcp.id,
        rsaKeyPair.private.encoded.keyToHexString().toPrivateKey(),
        pubKey.toPublicKey(),
        authJwt,
        jwtUtils.decodeAndGetClaims(authJwt).let { jwtUtils.jwtDetailsFromClaims(KmehrJWTDetails, it) }
    )
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
suspend fun createPatientUser(iCureUrl: String, username: String, password: String): UserCredentials {
    val login = generateEmail()
    val rsaKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
    val pubKey = rsaKeyPair.public.encoded.keyToHexString()

    val patient = PatientApi(basePath = iCureUrl, authProvider = JWTProvider(iCureUrl, username, password))
        .createPatient(
            PatientDto(
                id = uuid(),
                firstName = "patient",
                lastName = login,
                publicKey = pubKey
            )
        )

    val userPassword = uuid()
    val user = UserApi(basePath = iCureUrl, authProvider = JWTProvider(iCureUrl, username, password))
        .createUser(
            UserDto(
                id = uuid(),
                login = login,
                email = login,
                passwordHash = userPassword,
                patientId = patient.id
            )
        )

    val authJwt = getAuthJWT(iCureUrl, login, userPassword)

    return UserCredentials(
        user.id,
        login,
        userPassword,
        patient.id,
        rsaKeyPair.private.encoded.keyToHexString().toPrivateKey(),
        pubKey.toPublicKey(),
        authJwt
    )
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class)
suspend fun createAdminUser(iCureUrl: String, username: String, password: String): UserCredentials {
    val login = generateEmail()
    val rsaKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
    val pubKey = rsaKeyPair.public.encoded.keyToHexString()

    val userPassword = uuid()
    val user = UserApi(basePath = iCureUrl, authProvider = JWTProvider(iCureUrl, username, password))
        .createUser(
            UserDto(
                id = uuid(),
                login = login,
                email = login,
                passwordHash = userPassword
            )
        )

    PermissionApi(basePath = iCureUrl, authProvider = JWTProvider(iCureUrl, username, password))
        .modifyUserPermissions(
            "${user.groupId}:${user.id}",
            PermissionDto(
                grants = setOf(
                    AlwaysPermissionItemDto(
                        PermissionTypeDto.ADMIN
                    )
                )
            )
        )

    val authJwt = getAuthJWT(iCureUrl, login, userPassword)

    return UserCredentials(
        user.id,
        login,
        userPassword,
        null,
        rsaKeyPair.private.encoded.keyToHexString().toPrivateKey(),
        pubKey.toPublicKey(),
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