/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

import org.taktik.icure.services.external.rest.v1.dto.MessageDto

data class MessageWithBatch (
    val invoicesBatch: InvoicesBatch,
    val message: MessageDto
)
