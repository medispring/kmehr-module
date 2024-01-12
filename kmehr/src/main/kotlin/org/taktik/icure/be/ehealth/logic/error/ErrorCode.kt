/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.be.ehealth.logic.error

/**
 * @author Bernard Paulus on 23/05/17.
 */
enum class ErrorCode(val level: ErrorLevel) {
    /**
     * avoid using this: add your own error
     */
    GENERIC_ERROR(ErrorLevel.ERROR),
}
