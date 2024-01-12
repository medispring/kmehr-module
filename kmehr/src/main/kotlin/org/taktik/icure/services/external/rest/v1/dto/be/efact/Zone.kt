/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.taktik.icure.services.external.rest.v1.dto.be.efact.segments.ZoneDescription

@JsonIgnoreProperties("zoneDescription")
class Zone(var zoneDescription: ZoneDescription? = null, var value: Any? = null) {
    val description: String? = this.zoneDescription?.label
    val zone: String? = this.zoneDescription?.zones?.firstOrNull()
    override fun toString(): String {
        return "${(zoneDescription?.zones?.first() ?: "").padEnd(4)}[${zoneDescription!!.position.toString().padEnd(3)}]:\t$value"
    }
}
