package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.embed.EncryptedService
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.mapper.embed.ServiceV2Mapper
import org.springframework.stereotype.Service as SpringService

@SpringService
class ServiceMapper(
	objectMapper: ObjectMapper,
	serviceMapper: ServiceV2Mapper
) : AbstractEntityMapper<Service, EncryptedService, ServiceDto>(
	objectMapper,
	EncryptedService::class,
	ServiceDto::class.java,
	serviceMapper::map,
	serviceMapper::map
)