/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.samv2.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.samv2.embed.SamText
import org.taktik.icure.services.external.rest.v1.dto.samv2.embed.SamTextDto
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface SamTextMapper {
	fun map(samTextDto: SamTextDto): SamText
	fun map(samText: SamText): SamTextDto
}
