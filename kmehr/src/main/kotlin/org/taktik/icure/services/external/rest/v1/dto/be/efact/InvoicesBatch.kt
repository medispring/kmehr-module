/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

data class InvoicesBatch (
    val invoicingYear: Int = 0,
    val invoicingMonth: Int = 0,
    val fileRef: String? = null, // 13 alphanumeric internal reference. Typically, we use a base36 representation of the 16 first hex of the UUID id of the Message
    val batchRef: String? = null, // 25 alphanumeric internal reference. Typically, we use a base36 representation of the UUID id of the Message
    val ioFederationCode: String? = null, // 3 digits code of the IO federation
    val uniqueSendNumber: Long? = null, // 3 digits number for batch (typically the number of the day * 2 + 1 if 306)
    val sender: InvoiceSender? = null,
    val numericalRef: Long? = null,
    val invoices: List<EfactInvoice> = emptyList()
)
