package org.taktik.icure.test.fake.wscontrollers

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.web.reactive.socket.WebSocketSession
import org.taktik.icure.services.external.http.websocket.operation.Operation
import reactor.core.publisher.Mono

class PlainTextOperation(
    private val webSocket: WebSocketSession,
    private val objectMapper: ObjectMapper
) : Operation {
    override fun handle(message: String) {
        println(message)
    }

    suspend fun textResponse(text: String) {
        webSocket.send(Mono.just(webSocket.textMessage(text))).awaitFirstOrNull()
    }
}
