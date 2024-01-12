/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.samv2.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.samv2.embed.VirtualIngredient
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.VirtualIngredientDto
import org.taktik.icure.services.external.rest.v1.mapper.samv2.stub.SubstanceStubMapper

@Mapper(componentModel = "spring", uses = [StrengthRangeMapper::class, SubstanceStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface VirtualIngredientMapper {
	fun map(virtualIngredientDto: VirtualIngredientDto): VirtualIngredient
	fun map(virtualIngredient: VirtualIngredient): VirtualIngredientDto
}
