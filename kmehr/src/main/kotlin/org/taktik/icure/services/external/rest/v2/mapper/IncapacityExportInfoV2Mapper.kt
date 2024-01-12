package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mappings
import org.taktik.icure.domain.be.kmehr.IncapacityExportInfo
import org.taktik.icure.services.external.rest.v2.dto.be.kmehr.IncapacityExportInfoDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthcarePartyV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.ServiceV2Mapper

@Mapper(componentModel = "spring", uses = [HealthcarePartyV2Mapper::class, AddressV2Mapper::class, ServiceV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IncapacityExportInfoV2Mapper {
    @Mappings
    fun map(incapacityExportInfoDto: IncapacityExportInfoDto): IncapacityExportInfo
    fun map(incapacityExportInfo: IncapacityExportInfo): IncapacityExportInfoDto
}
