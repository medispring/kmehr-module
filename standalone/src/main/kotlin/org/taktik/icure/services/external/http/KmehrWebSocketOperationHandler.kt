package org.taktik.icure.services.external.http

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketSession
import org.taktik.icure.asynclogic.impl.BridgeAsyncSessionLogic
import org.taktik.icure.services.external.http.websocket.annotation.*
import org.taktik.icure.services.external.http.websocket.factory.DefaultWebSocketOperationFactoryImpl
import org.taktik.icure.services.external.http.websocket.operation.WebSocketOperationFactory
import reactor.core.publisher.Mono

@Component
class KmehrWebSocketOperationHandler(
    wsControllers: List<WsController>,
    objectMapper: ObjectMapper,
    sessionLogic: BridgeAsyncSessionLogic,
    operationFactories: List<WebSocketOperationFactory>,
    defaultFactory: DefaultWebSocketOperationFactoryImpl,
) : WebSocketOperationHandler(wsControllers, objectMapper, sessionLogic, operationFactories, defaultFactory) {

    private var prefix: String? = null

    private suspend fun authenticate(): Boolean =
        try {
            sessionLogic.getCurrentUserId()
            true
        } catch (_: Exception) { false }

    override fun handle(session: WebSocketSession): Mono<Void> = mono {
        val path = session.handshakeInfo.uri.path
            .replaceFirst("^" + (prefix?.toRegex() ?: ""), "")
            .replaceFirst(";.+?=.*".toRegex(), "")

        if (authenticate()) {
            val pathContainer = PathContainer.parsePath(session.handshakeInfo.uri.path)
            val pathMethod = methods[path]
                ?: methods.entries.find { (_, p) ->
                    p.first.matches(pathContainer)
                }?.value

            var operation = pathMethod?.let { pm ->
                val invocation = pm.second
                if (invocation.method.parameters.any { it.getAnnotation(WSParam::class.java) != null || it.getAnnotation(WSMessage::class.java) != null }) null
                else invocation.factory.get(invocation.operationClass, session).also {
                    launchOperation(it, pathMethod, pathContainer).awaitFirstOrNull()
                }
            }

            pathMethod?.let { pm ->
                session.receive().doOnNext { wsm -> wsm.retain() }.asFlow().collect { wsm ->
                    try {
                        val payloadAsText = wsm.payloadAsText

                        if (operation == null) {
                            val invocation = pm.second
                            invocation.factory.get(invocation.operationClass, session).also { operation = it }.let {
                                launch {
                                    try {
                                        launchOperation(it, pathMethod, pathContainer, wsm).awaitFirstOrNull()
                                    } finally {
                                        wsm.release()
                                    }
                                }
                            }
                        } else {
                            try {
                                //wsm.payloadAsText works for binary or text messages
                                operation!!.handle(payloadAsText)
                            } finally {
                                wsm.release()
                            }
                        }
                    } catch (e: Exception) {
                        handleOperationError(session, e)
                    }
                }
                operation?.complete()
            } ?: throw IllegalArgumentException("No operation found for path $path")
        }

        null
    }
}
