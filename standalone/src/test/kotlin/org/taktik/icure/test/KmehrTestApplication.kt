package org.taktik.icure.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.api.raw.impl.RawGroupApiImpl
import com.icure.sdk.api.raw.impl.RawHealthcarePartyApiImpl
import com.icure.sdk.api.raw.impl.RawPermissionApiImpl
import com.icure.sdk.api.raw.impl.RawUserApiImpl
import com.icure.sdk.model.DatabaseInitialisation
import com.icure.sdk.model.HealthcareParty
import com.icure.sdk.model.User
import com.icure.sdk.model.embed.GroupType
import com.icure.sdk.model.security.AlwaysPermissionItem
import com.icure.sdk.model.security.Permission
import com.icure.sdk.model.security.PermissionType
import com.icure.sdk.utils.InternalIcureApi
import com.icure.sdk.utils.Serialization
import com.icure.test.setup.ICureTestSetup
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.asynclogic.bridge.mappers.UserMapper
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.UserType
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.test.fake.components.FakeBridgeCredentialsManager
import java.io.File

@SpringBootApplication(
    scanBasePackages = [
        "org.springframework.boot.autoconfigure.aop",
        "org.springframework.boot.autoconfigure.context",
        "org.springframework.boot.autoconfigure.validation",
        "org.springframework.boot.autoconfigure.websocket",
        "org.taktik.icure.config",
        "org.taktik.icure.asynclogic",
        "org.taktik.icure.security",
        "org.taktik.icure.config",
        "org.taktik.icure.asynclogic",
        "org.taktik.icure.asyncdao",
        "org.taktik.icure.be.ehealth.logic",
        "org.taktik.icure.be.ehealth.logic.impl",
        "org.taktik.icure.be.format.logic",
        "org.taktik.icure.db",
        "org.taktik.icure.errors",
        "org.taktik.icure.services.external.rest",
        "org.taktik.icure.services.external.http",
        "org.taktik.icure.services.external.rest.v1.controllers",
        "org.taktik.icure.services.external.rest.v1.mapper",
        "org.taktik.icure.services.external.rest.v2.mapper",
        "org.taktik.icure.test",
        "org.taktik.icure.test.fake.controllers",
        "org.taktik.icure.test.fake.wscontrollers"
    ]
)
@PropertySource("classpath:kmehr-test.properties")
@TestConfiguration
class KmehrTestApplication {

    companion object {
        lateinit var groupId: String
        lateinit var masterHcp: UserCredentials
        lateinit var fakeSessionLogic: BridgeAsyncSessionLogic
    }

    @Value("\${icure.backend.url}")
    val baseICurePath = ""

    private val composeDir = "src/test/resources/docker"
    private val krakenCompose = System.getenv("KRAKEN_COMPOSE") ?: "file://$composeDir/docker-compose-cloud.yaml"

    @OptIn(InternalIcureApi::class)
    @Bean
    fun performStartupTasks(
        bridgeConfig: BridgeConfig,
        jwtUtils: JwtUtils,
        objectMapper: ObjectMapper,
        userMapper: UserMapper,
        internalDaos: List<InternalDAO<*>>
    ) = ApplicationRunner {
        runBlocking {
            ICureTestSetup.startKrakenEnvironment(krakenCompose, emptyList(), composeDir)
            ICureTestSetup.bootstrapCloud("xx", "xx", uuid(), "john", couchDbUser = "icure", couchDbPassword = "icure", rootUserRoles = defaultRoles) //pragma: allowlist secret
            loadRolesInConfig()
            val authProvider = getAuthProvider(baseICurePath, "john", "LetMeIn")
            val groupApi = RawGroupApiImpl(
                apiUrl = baseICurePath,
                authProvider = authProvider,
                httpClient = testHttpClient,
                json = Serialization.json
            )
            val userApi = RawUserApiImpl(
                apiUrl = baseICurePath,
                authProvider = authProvider,
                httpClient = testHttpClient,
                json = Serialization.json
            )
            val hcpApi = RawHealthcarePartyApiImpl(
                apiUrl = baseICurePath,
                authProvider = authProvider,
                httpClient = testHttpClient,
                json = Serialization.json
            )

            val testGroupId = groupApi.listGroups().successBody().firstOrNull{ it.id.startsWith("e2e-test") }?.id
                ?: "e2e-test-${uuid().subSequence(0,6)}".also {
                    groupApi.createGroup(
                        id = it,
                        name = "test",
                        type = null,
                        password = uuid(),
                        server = null,
                        q = null,
                        superGroup = null,
                        applicationId = null,
                        initialisationData = DatabaseInitialisation(null, null, null, null),
                    ).successBody()
                }

            val createdHcp = hcpApi.createHealthcarePartyInGroup(
                testGroupId,
                HealthcareParty(
                    uuid(),
                    name = "Mr. Darcy"
                )
            ).successBody()

            val userLogin = generateEmail()
            val userPwd = uuid()
            val createdUser = userApi.createUserInGroup(
                testGroupId,
                User(
                    uuid(),
                    login = userLogin,
                    email = userLogin,
                    passwordHash = userPwd,
                    healthcarePartyId = createdHcp.id
                )
            ).successBody()

            assignAdminPermissionToUser(testGroupId, createdUser.id)
            checkIfUserIsAvailable(userLogin, userPwd)

            internalDaos.forEach {
                it.forceInitStandardDesignDocument(true)
            }

            groupId = testGroupId
            masterHcp = UserCredentials(createdUser.id, userLogin, userPwd, createdHcp.id)
            fakeSessionLogic = BridgeAsyncSessionLogic(
                bridgeConfig,
                FakeBridgeCredentialsManager(bridgeConfig, masterHcp.login, masterHcp.password),
                jwtUtils,
                objectMapper
            )
        }
    }

    @OptIn(InternalIcureApi::class)
    private suspend fun checkIfUserIsAvailable(username: String, password: String) = flow<Unit> {
        val authProvider = getAuthProvider(baseICurePath, username, password)
        RawUserApiImpl(
            apiUrl = baseICurePath,
            authProvider = authProvider,
            httpClient = testHttpClient,
            json = Serialization.json
        ).getCurrentUser().successBody()
    }.retry(5) {
        delay(2_000)
        true
    }.collect()

    @OptIn(InternalIcureApi::class)
    private suspend fun assignAdminPermissionToUser(groupId: String, userId: String) = flow<Unit> {
        val authProvider = getAuthProvider(baseICurePath, "john", "LetMeIn")
        RawPermissionApiImpl(
            apiUrl = baseICurePath,
            authProvider = authProvider,
            httpClient = testHttpClient,
            json = Serialization.json
        ).modifyUserPermissions(
                "$groupId:$userId",
                Permission(
                    grants = setOf(
                        AlwaysPermissionItem(
                            PermissionType.Admin
                        )
                    )
                )
            )
    }.retry(5) {
        delay(2_000)
        true
    }.collect()

    private suspend fun loadRolesInConfig() {
        val payload = File("src/test/resources/roles.json").readText()
        val client = HttpClient()
        client.post("http://localhost:15984/icure-__-config/_bulk_docs") {
            basicAuth("icure", "icure")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
    }

    private val defaultRoles = mapOf(
        UserType.PATIENT.name to listOf("BASIC_USER", "BASIC_DATA_OWNER"),
        UserType.HCP.name to listOf("BASIC_USER", "BASIC_DATA_OWNER", "PATIENT_USER_MANAGER", "LEGACY_HCP"),
        UserType.DEVICE.name to listOf("BASIC_USER", "BASIC_DATA_OWNER"),
        UserType.USER.name to listOf("BASIC_USER")
    )
}
