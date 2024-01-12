package org.taktik.icure.security

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.taktik.icure.constants.Roles
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.security.jwt.EncodedJWTAuth
import org.taktik.icure.security.jwt.JwtUtils
import reactor.core.publisher.Mono

class CustomAuthenticationManager(
    private val jwtUtils: JwtUtils
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication?): Mono<Authentication> = mono {
        authentication
            .takeIf { it is EncodedJWTAuth }
            ?.let {
                try {
                    jwtUtils.decodeAndGetClaims((it as EncodedJWTAuth).token).let { claims ->
                        jwtUtils.jwtDetailsFromClaims(KmehrJWTDetails, claims)
                    }
                } catch (_: Exception) { null }
            }?.let { jwt ->
                EncodedJWTAuth(
                    token = (authentication as EncodedJWTAuth).token,
                    claims = jwt,
                    authorities = setOfNotNull(
                        SimpleGrantedAuthority(Roles.GrantedAuthority.ROLE_HCP).takeIf { jwt.dataOwnerType == DataOwnerType.HCP },
                        SimpleGrantedAuthority(Roles.GrantedAuthority.ROLE_ADMINISTRATOR).takeIf { jwt.isSuperAdmin },
                    ).toMutableSet()
                )
            }?.also {
                loadSecurityContext()?.map { ctx ->
                    ctx.authentication = it
                }?.awaitFirstOrNull()
            } ?: throw BadCredentialsException("Invalid username or password")
    }
}
