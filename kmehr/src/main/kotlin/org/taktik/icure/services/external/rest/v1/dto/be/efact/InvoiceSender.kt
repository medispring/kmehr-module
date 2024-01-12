/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

data class InvoiceSender (
    val nihii: Long? = null,
    val bic: String? = null,
    val iban: String? = null,
    val bce: Long? = 999999922L,
    val ssin: String? = null,
    val lastName: String? = null,
    val firstName: String? = null,
    val phoneNumber: Long? = null,
    val conventionCode: Int? = null,
    val specialist: Boolean = nihii?.let {it % 1000L >= 10 } ?: false
)
