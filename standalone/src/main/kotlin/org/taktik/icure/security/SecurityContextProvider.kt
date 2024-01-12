package org.taktik.icure.security

import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Mono
import kotlin.coroutines.coroutineContext

suspend fun loadSecurityContext(): Mono<SecurityContext>? {
    return coroutineContext[ReactorContext]?.context?.get<Mono<SecurityContext>>(SecurityContext::class.java)
}
