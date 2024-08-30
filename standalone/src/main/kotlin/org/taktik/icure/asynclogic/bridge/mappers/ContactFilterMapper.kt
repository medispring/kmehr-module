package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import com.icure.sdk.model.Contact as SdkContact
import org.springframework.stereotype.Service
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByDataOwnerFormIdsFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByDataOwnerPatientOpeningDateFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByExternalIdFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByServiceIdsFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyPatientTagCodeDateFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyTagCodeDateFilter
import org.taktik.icure.domain.filter.impl.contact.ContactByHcPartyIdentifiersFilter
import com.icure.sdk.model.filter.contact.ContactByDataOwnerFormIdsFilter as SdkContactByDataOwnerFormIdsFilter
import com.icure.sdk.model.filter.contact.ContactByDataOwnerPatientOpeningDateFilter as SdkContactByDataOwnerPatientOpeningDateFilter
import com.icure.sdk.model.filter.contact.ContactByExternalIdFilter as SdkContactByExternalIdFilter
import com.icure.sdk.model.filter.contact.ContactByHcPartyFilter as SdkContactByHcPartyFilter
import com.icure.sdk.model.filter.contact.ContactByServiceIdsFilter as SdkContactByServiceIdsFilter
import com.icure.sdk.model.filter.contact.ContactByHcPartyPatientTagCodeDateFilter as SdkContactByHcPartyPatientTagCodeDateFilter
import com.icure.sdk.model.filter.contact.ContactByHcPartyTagCodeDateFilter as SdkContactByHcPartyTagCodeDateFilter
import com.icure.sdk.model.filter.contact.ContactByHcPartyIdentifiersFilter as SdkContactByHcPartyIdentifiersFilter
import com.icure.sdk.model.filter.AbstractFilter as SdkAbstractFilter
import org.taktik.icure.entities.Contact

@Service
class ContactFilterMapper(
	objectMapper: ObjectMapper,
): AbstractFilterMapper<Contact, SdkContact>(objectMapper) {

	fun mapOrNull(filter: AbstractFilter<*>): SdkAbstractFilter<SdkContact>? = when(filter) {
		is ContactByDataOwnerFormIdsFilter -> doMap<ContactByDataOwnerFormIdsFilter, SdkContactByDataOwnerFormIdsFilter>(filter)
		is ContactByExternalIdFilter -> doMap<ContactByExternalIdFilter, SdkContactByExternalIdFilter>(filter)
		is ContactByDataOwnerPatientOpeningDateFilter -> doMap<ContactByDataOwnerPatientOpeningDateFilter, SdkContactByDataOwnerPatientOpeningDateFilter>(filter)
		is ContactByHcPartyFilter -> doMap<ContactByHcPartyFilter, SdkContactByHcPartyFilter>(filter)
		is ContactByServiceIdsFilter -> doMap<ContactByServiceIdsFilter, SdkContactByServiceIdsFilter>(filter)
		is ContactByHcPartyPatientTagCodeDateFilter -> doMap<ContactByHcPartyPatientTagCodeDateFilter, SdkContactByHcPartyPatientTagCodeDateFilter>(filter)
		is ContactByHcPartyTagCodeDateFilter -> doMap<ContactByHcPartyTagCodeDateFilter, SdkContactByHcPartyTagCodeDateFilter>(filter)
		is ContactByHcPartyIdentifiersFilter -> doMap<ContactByHcPartyIdentifiersFilter, SdkContactByHcPartyIdentifiersFilter>(filter)
		else -> null
	}
}