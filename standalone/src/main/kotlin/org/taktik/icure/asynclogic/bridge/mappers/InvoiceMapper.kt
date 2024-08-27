package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.EncryptedInvoice
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Invoice
import org.taktik.icure.services.external.rest.v2.dto.InvoiceDto
import org.taktik.icure.services.external.rest.v2.mapper.InvoiceV2Mapper

@Service
class InvoiceMapper(
	objectMapper: ObjectMapper,
	invoiceMapper: InvoiceV2Mapper
) : AbstractEntityMapper<Invoice, EncryptedInvoice, InvoiceDto>(
	objectMapper,
	EncryptedInvoice::class,
	InvoiceDto::class.java,
	invoiceMapper::map,
	invoiceMapper::map
)