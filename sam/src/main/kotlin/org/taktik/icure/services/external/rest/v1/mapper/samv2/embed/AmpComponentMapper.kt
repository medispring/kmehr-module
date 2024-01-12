/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.samv2.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.samv2.embed.AmpComponent
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.AmpComponentDto
import org.taktik.icure.services.external.rest.v1.mapper.samv2.stub.PharmaceuticalFormStubMapper

@Mapper(componentModel = "spring", uses = [IngredientMapper::class, RouteOfAdministrationMapper::class, PharmaceuticalFormStubMapper::class, SamTextMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface AmpComponentMapper {
	fun map(ampComponentDto: AmpComponentDto): AmpComponent
	fun map(ampComponent: AmpComponent): AmpComponentDto
}
