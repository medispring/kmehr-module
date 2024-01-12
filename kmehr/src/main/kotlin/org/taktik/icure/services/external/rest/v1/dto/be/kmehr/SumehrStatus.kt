/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.be.kmehr

import java.io.Serializable

enum class SumehrStatus : Serializable {
    absent, uptodate, outdated
}
