package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.EncryptedDocument
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Document
import org.taktik.icure.services.external.rest.v2.dto.DocumentDto
import org.taktik.icure.services.external.rest.v2.mapper.DocumentV2Mapper

@Service
class DocumentMapper(
	objectMapper: ObjectMapper,
	documentMapper: DocumentV2Mapper
) : AbstractEntityMapper<Document, EncryptedDocument, DocumentDto>(
	objectMapper,
	EncryptedDocument::class,
	DocumentDto::class.java,
	documentMapper::map,
	documentMapper::map
)