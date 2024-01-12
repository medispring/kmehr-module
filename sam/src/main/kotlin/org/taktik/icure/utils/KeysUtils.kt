package org.taktik.icure.utils

import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.db.sanitizeString

fun makeFromTo(label: String?, language: String?): Pair<ComplexKey, ComplexKey> {
    val sanitizedLabel = label?.let { sanitizeString(it) }
    val from = ComplexKey.of(
        language ?: "\u0000",
        sanitizedLabel ?: "\u0000"
    )
    val to = ComplexKey.of(
        language ?: ComplexKey.emptyObject(),
        if (sanitizedLabel == null) ComplexKey.emptyObject() else sanitizedLabel + "\ufff0"
    )
    return Pair(from, to)
}
