package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.Patient as SdkPatient
import org.springframework.stereotype.Service
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndExternalIdFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyDateOfBirthBetweenFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndActiveFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndSsinsFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndSsinFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndTelecomFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyDateOfBirthFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndAddressFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyGenderEducationProfession
import org.taktik.icure.domain.filter.impl.patient.PatientByIdsFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyNameFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByHcPartyAndIdentifiersFilter
import org.taktik.icure.domain.filter.impl.patient.PatientByDataOwnerModifiedAfterFilter
import org.taktik.icure.entities.Patient
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndExternalIdFilter as SdkPatientByHcPartyAndExternalIdFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyFilter as SdkPatientByHcPartyFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyDateOfBirthBetweenFilter as SdkPatientByHcPartyDateOfBirthBetweenFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndActiveFilter as SdkPatientByHcPartyAndActiveFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndSsinsFilter as SdkPatientByHcPartyAndSsinsFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndSsinFilter as SdkPatientByHcPartyAndSsinFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndTelecomFilter as SdkPatientByHcPartyAndTelecomFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyDateOfBirthFilter as SdkPatientByHcPartyDateOfBirthFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndAddressFilter as SdkPatientByHcPartyAndAddressFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyGenderEducationProfession as SdkPatientByHcPartyGenderEducationProfession
import com.icure.sdk.model.filter.patient.PatientByIdsFilter as SdkPatientByIdsFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyNameFilter as SdkPatientByHcPartyNameFilter
import com.icure.sdk.model.filter.patient.PatientByHcPartyAndIdentifiersFilter as SdkPatientByHcPartyAndIdentifiersFilter
import com.icure.sdk.model.filter.patient.PatientByDataOwnerModifiedAfterFilter as SdkPatientByDataOwnerModifiedAfterFilter
import com.icure.sdk.model.filter.AbstractFilter as SdkAbstractFilter

@Service
class PatientFilterMapper(
	objectMapper: ObjectMapper
) : AbstractFilterMapper<Patient, SdkPatient>(objectMapper) {

	fun mapOrNull(filter: AbstractFilter<*>): SdkAbstractFilter<SdkPatient>? = when(filter) {
		is PatientByHcPartyAndExternalIdFilter -> doMap<PatientByHcPartyAndExternalIdFilter, SdkPatientByHcPartyAndExternalIdFilter>(filter)
		is PatientByHcPartyFilter -> doMap<PatientByHcPartyFilter, SdkPatientByHcPartyFilter>(filter)
		is PatientByHcPartyDateOfBirthBetweenFilter -> doMap<PatientByHcPartyDateOfBirthBetweenFilter, SdkPatientByHcPartyDateOfBirthBetweenFilter>(filter)
		is PatientByHcPartyAndActiveFilter -> doMap<PatientByHcPartyAndActiveFilter, SdkPatientByHcPartyAndActiveFilter>(filter)
		is PatientByHcPartyAndSsinsFilter -> doMap<PatientByHcPartyAndSsinsFilter, SdkPatientByHcPartyAndSsinsFilter>(filter)
		is PatientByHcPartyAndSsinFilter -> doMap<PatientByHcPartyAndSsinFilter, SdkPatientByHcPartyAndSsinFilter>(filter)
		is PatientByHcPartyAndTelecomFilter -> doMap<PatientByHcPartyAndTelecomFilter, SdkPatientByHcPartyAndTelecomFilter>(filter)
		is PatientByHcPartyDateOfBirthFilter -> doMap<PatientByHcPartyDateOfBirthFilter, SdkPatientByHcPartyDateOfBirthFilter>(filter)
		is PatientByHcPartyAndAddressFilter -> doMap<PatientByHcPartyAndAddressFilter, SdkPatientByHcPartyAndAddressFilter>(filter)
		is PatientByHcPartyGenderEducationProfession -> doMap<PatientByHcPartyGenderEducationProfession, SdkPatientByHcPartyGenderEducationProfession>(filter)
		is PatientByIdsFilter -> doMap<PatientByIdsFilter, SdkPatientByIdsFilter>(filter)
		is PatientByHcPartyNameFilter -> doMap<PatientByHcPartyNameFilter, SdkPatientByHcPartyNameFilter>(filter)
		is PatientByHcPartyAndIdentifiersFilter -> doMap<PatientByHcPartyAndIdentifiersFilter, SdkPatientByHcPartyAndIdentifiersFilter>(filter)
		is PatientByDataOwnerModifiedAfterFilter -> doMap<PatientByDataOwnerModifiedAfterFilter, SdkPatientByDataOwnerModifiedAfterFilter>(filter)
		else -> null
	}
}