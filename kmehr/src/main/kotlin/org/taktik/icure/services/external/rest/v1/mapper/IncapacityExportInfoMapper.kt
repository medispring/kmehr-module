package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mappings
import org.taktik.icure.domain.be.kmehr.IncapacityExportInfo
import org.taktik.icure.services.external.rest.v1.dto.be.kmehr.IncapacityExportInfoDto
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.ServiceMapper

@Mapper(componentModel = "spring", uses = [HealthcarePartyMapper::class, AddressMapper::class, ServiceMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IncapacityExportInfoMapper {
    @Mappings
    fun map(incapacityExportInfoDto: IncapacityExportInfoDto): IncapacityExportInfo
    fun map(incapacityExportInfo: IncapacityExportInfo): IncapacityExportInfoDto
}
