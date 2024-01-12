package org.taktik.icure.errors

import org.springframework.security.core.AuthenticationException

class UnauthorizedException(msg: String) : AuthenticationException(msg)
