/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.be.efact

import java.util.Calendar
import java.util.Date

data class EIDItem(
    val deviceType: String = "1",
    val readDate: Long? = Date().time,
    val readHour: Int? = Calendar.getInstance().let { cal -> cal.get(Calendar.HOUR_OF_DAY) * 100 + cal.get(Calendar.MINUTE) },
    val readType: String = "1",
    val readValue: String? = null
)
