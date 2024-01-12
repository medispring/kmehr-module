/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.samv2.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.samv2.embed.Quantity
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.QuantityDto
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface QuantityMapper {
	fun map(quantityDto: QuantityDto): Quantity
	fun map(quantity: Quantity): QuantityDto
}
