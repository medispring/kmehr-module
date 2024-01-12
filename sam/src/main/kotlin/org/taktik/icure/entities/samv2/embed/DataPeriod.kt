/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.samv2.embed

import java.io.Serializable

interface DataPeriod : Serializable {
	val from: Long?
	val to: Long?
}
