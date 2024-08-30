package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.Code as SdkCode
import org.springframework.stereotype.Service
import org.taktik.icure.entities.base.Code
import org.taktik.icure.services.external.rest.v2.dto.CodeDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeV2Mapper

@Service
class CodeMapper(
	objectMapper: ObjectMapper,
	codeMapper: CodeV2Mapper
) : AbstractEntityMapper<Code, SdkCode, CodeDto>(
	objectMapper,
	SdkCode::class,
	CodeDto::class.java,
	codeMapper::map,
	codeMapper::map
)
