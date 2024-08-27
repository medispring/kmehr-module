package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.FormTemplate as SdkFormTemplate
import org.springframework.stereotype.Service
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.services.external.rest.v2.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v2.mapper.FormTemplateV2Mapper

@Service
class FormTemplateMapper(
	objectMapper: ObjectMapper,
	formTemplateMapper: FormTemplateV2Mapper
) : AbstractEntityMapper<FormTemplate, SdkFormTemplate, FormTemplateDto>(
	objectMapper,
	SdkFormTemplate::class,
	FormTemplateDto::class.java,
	formTemplateMapper::map,
	formTemplateMapper::map
)