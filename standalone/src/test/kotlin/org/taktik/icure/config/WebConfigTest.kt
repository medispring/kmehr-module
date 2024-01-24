package org.taktik.icure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.web.server.LocalServerPort
import org.taktik.icure.entities.base.Code
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.TestHttpClient
import org.taktik.icure.test.getAuthJWT
import org.taktik.icure.test.uuid
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebConfigTest(
    @LocalServerPort val port: Int,
    private val httpClient: TestHttpClient,
    private val bridgeConfig: BridgeConfig,
    private val objectMapper: ObjectMapper
) : BaseKmehrTest() {

    val khmerUrl by lazy { "http://127.0.0.1:$port" }

    init {
        webConfigTest()
    }

    private fun StringSpec.webConfigTest() {

        "Can send a very big payload (~1Mb)" {
            val text = File("src/test/resources/bigPayload.txt").readText()
            val code = Code(id = uuid(), data = text)
            val token = getAuthJWT(
                bridgeConfig.iCureUrl,
                KmehrTestApplication.masterHcp.login,
                KmehrTestApplication.masterHcp.password
            )
            httpClient.makePostRequest(
                "$khmerUrl/rest/test/big",
                objectMapper.writeValueAsString(code),
                mapOf("Authorization" to "Bearer $token")
            ).let {
                objectMapper.readValue<Code>(it)
            } shouldBe code
        }

    }

}