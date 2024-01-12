/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.http.websocket.operation

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.commons.logging.LogFactory
import org.springframework.web.reactive.socket.WebSocketSession
import org.taktik.icure.services.external.api.AsyncDecrypt
import org.taktik.icure.services.external.http.websocket.Message
import reactor.core.publisher.Mono
import java.io.IOException
import java.io.Serializable
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class KmehrFileOperation(webSocket: WebSocketSession, objectMapper: ObjectMapper) : BinaryOperation(webSocket, objectMapper), AsyncDecrypt {
    private val log = LogFactory.getLog(KmehrFileOperation::class.java)
    private val decodingSessions: MutableMap<String?, DecodingSession<*>> = HashMap()

    @Throws(IOException::class)
    override suspend fun <K : Serializable?> decrypt(encrypted: List<K>, clazz: Class<K>): List<K> {
        val uuid = UUID.randomUUID().toString()

        val message: Message<*> = Message("decrypt", clazz.simpleName, uuid, encrypted)
        val future = CompletableFuture<List<K>>()
        val decodingSession = DecodingSession(future, clazz)

        decodingSessions[uuid] = decodingSession
        log.info("Submit $uuid for decryption")
        val jsonMessage = objectMapper.writeValueAsString(message)
        val wsMessage = if (jsonMessage.length > 65000) webSocket.binaryMessage { it.wrap(jsonMessage.toByteArray(Charsets.UTF_8)) } else webSocket.textMessage(objectMapper.writeValueAsString(message))
        webSocket.send(Mono.just(wsMessage)).awaitFirstOrNull()
        return try {
            val result = Mono.fromFuture(future).timeout(Duration.ofSeconds(240)).awaitFirst()
            result
        } catch (toe: TimeoutException) {
            log.warn("Timeout on operation $uuid, decrypt ${clazz.simpleName}, no response received from client")
            listOf()
        } finally {
            decodingSessions.remove(uuid)
        }
    }

    override fun handle(message: String) {
        val dto = try {
            objectMapper.readTree(message)
        } catch (e: Exception) {
            log.error("Cannot parse because of $e. Object is: $message", e)
            throw (e)
        }
        if (dto["command"].asText() == "decryptResponse") {
            val uuid = dto["uuid"].asText()
            log.info("Received response for uuid $uuid")
            decodingSessions[uuid]?.let { session ->
                val decodingSession = session as DecodingSession<Serializable>
                val result = dto["body"].mapNotNull { jsonObject ->
                    try {
                        val value = objectMapper.treeToValue(jsonObject, decodingSession.clazz)
                        value
                    } catch (ee: Exception) {
                        log.error("Cannot parse because of $ee")
                        null
                    }
                }
                decodingSession.future.complete(result)
            } ?: log.warn("Missing session for uuid $uuid")
        }
    }

    private inner class DecodingSession<K : Serializable?> constructor(var future: CompletableFuture<List<K>>, var clazz: Class<K>)
}
