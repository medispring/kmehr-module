package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.EncryptedPatient
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Patient
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper

@Service
class PatientMapper(
	objectMapper: ObjectMapper,
	patientMapper: PatientV2Mapper
) : AbstractEntityMapper<Patient, EncryptedPatient, PatientDto>(
	objectMapper,
	EncryptedPatient::class,
	PatientDto::class.java,
	patientMapper::map,
	patientMapper::map
)