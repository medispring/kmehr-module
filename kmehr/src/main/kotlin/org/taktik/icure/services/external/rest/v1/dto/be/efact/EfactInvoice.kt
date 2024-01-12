/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

import org.taktik.icure.services.external.rest.v1.dto.PatientDto
import java.util.LinkedList

data class EfactInvoice (
    val patient: PatientDto? = null,
    val ioCode: String? = null,
    val items: MutableList<InvoiceItem> = LinkedList(),
    val reason: InvoicingTreatmentReasonCode? = null,
    val invoiceRef: String? = null,
    val invoiceNumber: Long? = null,
    val ignorePrescriptionDate: Boolean = false,
    val hospitalisedPatient: Boolean = false,
    val creditNote: Boolean = false,
    val relatedInvoiceIoCode: String? = null,
    val relatedInvoiceNumber: Long? = null,
    val relatedBatchSendNumber: Long? = null,
    val relatedBatchYearMonth: Long? = null
)
