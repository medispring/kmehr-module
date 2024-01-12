/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.api

import java.io.Serializable

interface AsyncDecrypt {

    suspend fun <K : Serializable?> decrypt(encrypted: List<K>, clazz: Class<K>): List<K>
}
