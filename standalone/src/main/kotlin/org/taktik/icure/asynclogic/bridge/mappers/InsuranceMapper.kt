package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.Insurance as SdkInsurance
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Insurance
import org.taktik.icure.services.external.rest.v2.dto.InsuranceDto
import org.taktik.icure.services.external.rest.v2.mapper.InsuranceV2Mapper

@Service
class InsuranceMapper(
	objectMapper: ObjectMapper,
	insuranceMapper: InsuranceV2Mapper
) : AbstractEntityMapper<Insurance, SdkInsurance, InsuranceDto>(
	objectMapper,
	SdkInsurance::class,
	InsuranceDto::class.java,
	insuranceMapper::map,
	insuranceMapper::map
)