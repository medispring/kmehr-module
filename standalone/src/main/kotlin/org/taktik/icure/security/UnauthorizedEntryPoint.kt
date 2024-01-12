package org.taktik.icure.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UnauthorizedEntryPoint : ServerAuthenticationEntryPoint {

    private val objectMapper: ObjectMapper by lazy { ObjectMapper() }

    override fun commence(exchange: ServerWebExchange, e: AuthenticationException): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        val body = objectMapper.writeValueAsString(mapOf("message" to e.message)).toByteArray()
        val buffer = exchange.response.bufferFactory().wrap(body)
        return exchange.response.writeWith(Flux.just(buffer))
    }
}
