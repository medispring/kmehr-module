/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact.segments

class ZoneDescription(
    var label: String? = null,
    var position: Int? = null,
    var length: Int? = null,
    var type: ZoneType? = null,
    var zones: List<String>? = null,
    var value: String? = null,
    var cs: Boolean = false,
) {
    enum class ZoneType(val symbol: String) {
        ALPHANUMERICAL("A"),
        NUMERICAL("N"),
    }
}
