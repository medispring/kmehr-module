package org.taktik.icure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.web.server.LocalServerPort
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.TestHttpClient
import org.taktik.icure.test.createAdminUser
import org.taktik.icure.test.getAuthJWT

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticationE2ETest(
    @LocalServerPort val port: Int,
    val httpClient: TestHttpClient,
    val bridgeConfig: BridgeConfig
) : BaseKmehrTest() {

    val khmerUrl by lazy { "http://127.0.0.1:$port" }
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    init {
        testAuthentication()
    }

    private fun StringSpec.testAuthentication() {
        "Can use the JWT issued by the Kraken" {
            val token = getAuthJWT(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password
            )
            val currentUserRaw = httpClient.makeGetRequest(
                "$khmerUrl/rest/test/auth/currentuser",
                mapOf("Authorization" to "Bearer $token")
            )
            val currentUser = objectMapper.readValue<UserDto>(currentUserRaw)
            currentUser.id shouldBe KmehrTestApplication.masterHcp.userId
        }

        "A request with no JWT results in a 401" {
            httpClient.makeGetRequest("$khmerUrl/rest/test/auth/currentuser", expectedResponse = 401)
        }

        "A request with an invalid JWT results in a 401" {
            httpClient.makeGetRequest(
                "$khmerUrl/rest/test/auth/currentuser",
                mapOf("Authorization" to "Bearer not.a.valid-token"),
                expectedResponse = 401
            )
        }

        "Can access the endpoints as an Admin" {
            val adminUser = createAdminUser(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password
            )
            httpClient.makeGetRequest(
                "$khmerUrl/rest/v2/be_kmehr/jwt/isValid",
                mapOf("Authorization" to "Bearer ${adminUser.authJWT}"),
                expectedResponse = 200
            )
        }
    }

}
