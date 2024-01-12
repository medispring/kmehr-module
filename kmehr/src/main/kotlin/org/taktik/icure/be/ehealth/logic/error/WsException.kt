/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.error

/**
 * @author Bernard Paulus on 23/05/17.
 */
class WsException(message: String? = null, exception: Throwable? = null, val error: ErrorCode, val arguments: List<Any> = listOf()) : RuntimeException(message, exception) {
    override fun toString(): String {
        return "WsException(error=$error, arguments=$arguments, super=${super.toString()})"
    }
}
