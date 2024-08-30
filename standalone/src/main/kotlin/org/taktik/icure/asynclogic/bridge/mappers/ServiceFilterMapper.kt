package org.taktik.icure.asynclogic.bridge.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.impl.service.ServiceByAssociationIdFilter
import org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyTagCodeDateFilter
import org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyIdentifiersFilter
import org.taktik.icure.domain.filter.impl.service.ServiceByIdsFilter
import org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyFilter
import org.taktik.icure.domain.filter.impl.service.ServiceByHcPartyHealthElementIdsFilter
import org.taktik.icure.domain.filter.impl.service.ServiceBySecretForeignKeys
import org.taktik.icure.domain.filter.impl.service.ServiceByQualifiedLinkFilter
import com.icure.sdk.model.filter.service.ServiceByAssociationIdFilter as SdkServiceByAssociationIdFilter
import com.icure.sdk.model.filter.service.ServiceByHcPartyTagCodeDateFilter as SdkServiceByHcPartyTagCodeDateFilter
import com.icure.sdk.model.filter.service.ServiceByHcPartyIdentifiersFilter as SdkServiceByHcPartyIdentifiersFilter
import com.icure.sdk.model.filter.service.ServiceByIdsFilter as SdkServiceByIdsFilter
import com.icure.sdk.model.filter.service.ServiceByHcPartyFilter as SdkServiceByHcPartyFilter
import com.icure.sdk.model.filter.service.ServiceByHcPartyHealthElementIdsFilter as SdkServiceByHcPartyHealthElementIdsFilter
import com.icure.sdk.model.filter.service.ServiceBySecretForeignKeys as SdkServiceBySecretForeignKeys
import com.icure.sdk.model.filter.service.ServiceByQualifiedLinkFilter as SdkServiceByQualifiedLinkFilter
import org.taktik.icure.entities.embed.Service
import org.springframework.stereotype.Service as SpringService
import com.icure.sdk.model.embed.Service as SdkService
import com.icure.sdk.model.filter.AbstractFilter as SdkAbstractFilter

@SpringService
class ServiceFilterMapper(
	objectMapper: ObjectMapper
): AbstractFilterMapper<Service, SdkService>(objectMapper) {

	fun mapOrNull(filter: AbstractFilter<*>): SdkAbstractFilter<SdkService>? = when(filter) {
		is ServiceByAssociationIdFilter -> doMap<ServiceByAssociationIdFilter, SdkServiceByAssociationIdFilter>(filter)
		is ServiceByHcPartyTagCodeDateFilter -> doMap<ServiceByHcPartyTagCodeDateFilter, SdkServiceByHcPartyTagCodeDateFilter>(filter)
		is ServiceByHcPartyIdentifiersFilter -> doMap<ServiceByHcPartyIdentifiersFilter, SdkServiceByHcPartyIdentifiersFilter>(filter)
		is ServiceByIdsFilter -> doMap<ServiceByIdsFilter, SdkServiceByIdsFilter>(filter)
		is ServiceByHcPartyFilter -> doMap<ServiceByHcPartyFilter, SdkServiceByHcPartyFilter>(filter)
		is ServiceByHcPartyHealthElementIdsFilter -> doMap<ServiceByHcPartyHealthElementIdsFilter, SdkServiceByHcPartyHealthElementIdsFilter>(filter)
		is ServiceBySecretForeignKeys -> doMap<ServiceBySecretForeignKeys, SdkServiceBySecretForeignKeys>(filter)
		is ServiceByQualifiedLinkFilter -> doMap<ServiceByQualifiedLinkFilter, SdkServiceByQualifiedLinkFilter>(filter)
		else -> null
	}

}