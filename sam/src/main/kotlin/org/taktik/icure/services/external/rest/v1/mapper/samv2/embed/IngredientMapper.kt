/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.samv2.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.samv2.embed.Ingredient
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.IngredientDto
import org.taktik.icure.services.external.rest.v1.mapper.samv2.stub.SubstanceStubMapper

@Mapper(componentModel = "spring", uses = [QuantityMapper::class, SubstanceStubMapper:: class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IngredientMapper {
	fun map(ingredientDto: IngredientDto): Ingredient
	fun map(ingredient: Ingredient): IngredientDto
}
