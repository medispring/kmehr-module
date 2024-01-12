/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact.segments

class RecordOrSegmentDescription {
    var zoneDescriptions: List<ZoneDescription>? = null
    override fun toString(): String {
        return zoneDescriptions?.firstOrNull()?.value ?: "-"
    }
}
