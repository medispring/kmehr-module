/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

class ErrorDetail : Serializable {
    @Schema(defaultValue = "0")
    var creationDate: Int = 0
    var errorCodeComment: String? = null

    @Schema(defaultValue = "0")
    var index: Int = 0

    @Schema(defaultValue = "0")
    var invoicingYearMonth: Int = 0

    @Schema(defaultValue = "0")
    var mutualityCode: Int = 0
    var oaResult: String? = null
    var rejectionCode1: String? = null
    var rejectionCode2: String? = null
    var rejectionCode3: String? = null
    var rejectionLetter1: String? = null
    var rejectionLetter2: String? = null
    var rejectionLetter3: String? = null
    var rejectionDescr1: String? = null
    var rejectionDescr2: String? = null
    var rejectionDescr3: String? = null
    var rejectionZoneDescr1: String? = null
    var rejectionZoneDescr2: String? = null
    var rejectionZoneDescr3: String? = null
    var reserve: String? = null

    @Schema(defaultValue = "0")
    var sendingId: Int = 0
    var zone114: String? = null
    var zone115: String? = null
    var zone116: String? = null
    var zone117: String? = null
    var zone118: String? = null
}
