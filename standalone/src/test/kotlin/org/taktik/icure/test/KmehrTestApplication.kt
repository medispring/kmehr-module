package org.taktik.icure.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.test.setup.ICureTestSetup
import io.icure.kraken.client.apis.GroupApi
import io.icure.kraken.client.apis.HealthcarePartyApi
import io.icure.kraken.client.apis.PermissionApi
import io.icure.kraken.client.apis.UserApi
import io.icure.kraken.client.security.BasicAuthProvider
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.entities.UserType
import org.taktik.icure.security.jwt.JwtUtils
import org.taktik.icure.services.external.rest.v2.dto.DatabaseInitialisationDto
import org.taktik.icure.services.external.rest.v2.dto.HealthcarePartyDto
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.security.AlwaysPermissionItemDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionTypeDto
import org.taktik.icure.services.external.rest.v2.mapper.UnsecureUserV2MapperImpl
import org.taktik.icure.test.fake.components.FakeBridgeCredentialsManager
import java.io.File

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
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
open class KmehrTestApplication {

    companion object {
        lateinit var groupId: String
        lateinit var masterHcp: UserCredentials
        lateinit var fakeSessionLogic: BridgeAsyncSessionLogic
    }

    @Value("\${icure.backend.url}")
    val baseICurePath = ""

    private val composeDir = "src/test/resources/docker"
    private val krakenCompose = System.getenv("KRAKEN_COMPOSE") ?: "file://$composeDir/docker-compose-cloud.yaml"

    @Bean
    open fun performStartupTasks(
        bridgeConfig: BridgeConfig,
        jwtUtils: JwtUtils,
        objectMapper: ObjectMapper,
        userMapper: UnsecureUserV2MapperImpl,
        internalDaos: List<InternalDAO<*>>
    ) = ApplicationRunner {
        runBlocking {
            ICureTestSetup.startKrakenEnvironment(krakenCompose, emptyList(), composeDir)
            ICureTestSetup.bootstrapCloud("xx", "xx", uuid(), "john", couchDbUser = "icure", couchDbPassword = "icure", rootUserRoles = defaultRoles) //pragma: allowlist secret
            loadRolesInConfig()
            val groupApi = GroupApi(basePath = baseICurePath, authProvider = BasicAuthProvider("john", "LetMeIn"))
            val userApi = UserApi(basePath = baseICurePath, authProvider = BasicAuthProvider("john", "LetMeIn"))
            val hcpApi = HealthcarePartyApi(basePath = baseICurePath, authProvider = BasicAuthProvider("john", "LetMeIn"))

            val testGroupId = groupApi.listGroups().firstOrNull{ it.id.startsWith("e2e-test") }?.id
                ?: "e2e-test-${uuid().subSequence(0,6)}".also {
                    groupApi.createGroup(
                        it,
                        "test",
                        uuid(),
                        DatabaseInitialisationDto(null, null, null, null),
                        null, null, null, null)
                }

            val createdHcp = hcpApi.createHealthcarePartyInGroup(
                testGroupId,
                HealthcarePartyDto(
                    uuid(),
                    name = "Mr. Darcy"
                )
            )

            val userLogin = generateEmail()
            val createdUser = userApi.createUserInGroup(
                testGroupId,
                UserDto(
                    uuid(),
                    login = userLogin,
                    email = userLogin,
                    healthcarePartyId = createdHcp.id
                )
            )

            assignAdminPermissionToUser(testGroupId, createdUser.id)
            val userPwd = userApi.getTokenInGroup(testGroupId, createdUser.id, uuid(), uuid(), 24 * 60 * 60)
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

    private suspend fun checkIfUserIsAvailable(username: String, password: String) = flow<Unit> {
        UserApi(baseICurePath, authProvider = BasicAuthProvider(username, password)).getCurrentUser()
    }.retry(5) {
        delay(2_000)
        true
    }.collect()

    private suspend fun assignAdminPermissionToUser(groupId: String, userId: String) = flow<Unit> {
        PermissionApi(basePath = baseICurePath, authProvider = BasicAuthProvider("john", "LetMeIn"))
            .modifyUserPermissions(
                "$groupId:$userId",
                PermissionDto(
                    grants = setOf(
                        AlwaysPermissionItemDto(
                            PermissionTypeDto.ADMIN
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
        UserType.PATIENT.name to "BASIC_USER\", \"BASIC_DATA_OWNER",
        UserType.HCP.name to "BASIC_USER\", \"BASIC_DATA_OWNER\", \"PATIENT_USER_MANAGER\", \"LEGACY_HCP",
        UserType.DEVICE.name to "BASIC_USER\", \"BASIC_DATA_OWNER",
        UserType.USER.name to "BASIC_USER"
    )
}
