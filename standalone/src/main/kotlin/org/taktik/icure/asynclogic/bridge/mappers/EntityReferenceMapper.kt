package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.services.external.rest.v2.dto.EntityReferenceDto
import org.taktik.icure.services.external.rest.v2.mapper.EntityReferenceV2Mapper
import com.icure.sdk.model.EntityReference as SdkEntityReference

@Service
class EntityReferenceMapper(
	objectMapper: ObjectMapper,
	entityReferenceMapper: EntityReferenceV2Mapper
) : AbstractEntityMapper<EntityReference, SdkEntityReference, EntityReferenceDto>(
	objectMapper,
	SdkEntityReference::class,
	EntityReferenceDto::class.java,
	entityReferenceMapper::map,
	entityReferenceMapper::map
)