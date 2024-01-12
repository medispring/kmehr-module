/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class NoCloseInputStream(`is`: InputStream?) : FilterInputStream(`is`) {
    @Throws(IOException::class)
    override fun close() {
        // Ignore. Use doClose() to close
    }

    @Throws(IOException::class)
    fun doClose() {
        super.close()
    }
}
