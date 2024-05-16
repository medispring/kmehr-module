package org.taktik.icure.service.external.http.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.web.server.LocalServerPort
import org.taktik.icure.asynclogic.impl.JwtResponse
import org.taktik.icure.config.BridgeConfig
import org.taktik.icure.services.external.rest.v2.dto.LoginCredentials
import org.taktik.icure.test.BaseKmehrTest
import org.taktik.icure.test.KmehrTestApplication
import org.taktik.icure.test.getAuthJWT
import org.taktik.icure.test.uuid

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebsocketAuthTest(
    @LocalServerPort val port: Int,
    val bridgeConfig: BridgeConfig,
    private val objectMapper: ObjectMapper
) : BaseKmehrTest() {

    val client = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) {
            json()
        }
    }

    init {
        testWebSocket()
    }

    private fun StringSpec.testWebSocket() {
        "Can use JWT authentication on WebSocket using Authorization header" {
            val token = getAuthJWT(bridgeConfig.iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password)
            client.webSocket(host = "127.0.0.1", port = port, path = "/ws/fake/echo", request = {
                headers.append("Authorization", "Bearer $token")
            }) {
                send(Frame.Text(uuid()))
                val echoedMessage = incoming.receiveCatching().getOrNull() as? Frame.Text ?: error("No message")
                echoedMessage.readText() shouldBe KmehrTestApplication.masterHcp.dataOwnerId
            }
        }

        "Can use JWT authentication on WebSocket using a path parameter" {
            val token = getAuthJWT(bridgeConfig.iCureUrl, KmehrTestApplication.masterHcp.login, KmehrTestApplication.masterHcp.password)
            client.webSocket(host = "127.0.0.1", port = port, path = "/ws/fake/echo?jwt=$token") {
                send(Frame.Text(uuid()))
                val echoedMessage = incoming.receiveCatching().getOrNull() as? Frame.Text ?: error("No message")
                echoedMessage.readText() shouldBe KmehrTestApplication.masterHcp.dataOwnerId
            }
        }

        "If the JWT sent to the WS operation expires, is automatically refreshed mid-request" {
            val shortLivedToken = client.post("${bridgeConfig.iCureUrl}/rest/v2/auth/login?duration=5") {
                contentType(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(LoginCredentials(
                        username = "${KmehrTestApplication.groupId}/${KmehrTestApplication.masterHcp.userId}",
                        password = KmehrTestApplication.masterHcp.password
                    )
                ))
            }.let { objectMapper.readValue<JwtResponse>(it.bodyAsText()) }.token
            client.webSocket(host = "127.0.0.1", port = port, path = "/ws/fake/slowOp?jwt=$shortLivedToken") {
                send(Frame.Text(uuid()))
                val echoedMessage = incoming.receiveCatching().getOrNull() as? Frame.Text ?: error("No message")
                echoedMessage.readText() shouldBe KmehrTestApplication.masterHcp.dataOwnerId
            }
        }
    }

}
