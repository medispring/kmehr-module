/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.samv2

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.samv2.stub.VmpGroupStub
import org.taktik.icure.services.external.rest.v1.dto.samv2.stub.VmpGroupStubDto
import org.taktik.icure.services.external.rest.v1.mapper.samv2.embed.SamTextMapper

@Mapper(componentModel = "spring", uses = [SamTextMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface VmpGroupStubMapper {
	fun map(vmpGroupStubDto: VmpGroupStubDto): VmpGroupStub
	@Mappings(
		Mapping(target = "productId", ignore = true)
	)
	fun map(vmpGroupStub: VmpGroupStub): VmpGroupStubDto
}
