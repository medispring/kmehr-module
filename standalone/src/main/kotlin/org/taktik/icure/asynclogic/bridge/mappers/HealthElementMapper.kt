package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.EncryptedHealthElement
import org.springframework.stereotype.Service
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthElementV2Mapper

@Service
class HealthElementMapper(
	objectMapper: ObjectMapper,
	healthElementMapper: HealthElementV2Mapper
) : AbstractEntityMapper<HealthElement, EncryptedHealthElement, HealthElementDto>(
	objectMapper,
	EncryptedHealthElement::class,
	HealthElementDto::class.java,
	healthElementMapper::map,
	healthElementMapper::map
)