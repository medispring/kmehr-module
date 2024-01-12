/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

data class InvoiceItem (
    val dateCode: Long? = null,
    val codeNomenclature: Long = 0,
    val relatedCode: Long? = null,
    val eidItem: EIDItem? = null,
    val insuranceRef: String? = null,
    val insuranceRefDate: Long? = null,
    val units: Int = 0,
    val reimbursedAmount: Long = 0,
    val patientFee: Long = 0,
    val doctorSupplement: Long = 0,
    val sideCode: InvoicingSideCode? = null,
    val timeOfDay: InvoicingTimeOfDay? = null,
    val override3rdPayerCode: Int? = null,
    val gnotionNihii: String? = null,
    val derogationMaxNumber: InvoicingDerogationMaxNumberCode? = null,
    val prescriberNorm: InvoicingPrescriberCode? = null,
    val prescriberNihii: String? = null,
    val prescriptionDate: Long? = null,
    val personalInterventionCoveredByThirdPartyCode: Int? = null,
    val doctorIdentificationNumber: String? = null,
    val invoiceRef: String? = null,
    val percentNorm: InvoicingPercentNorm? = null,
)
