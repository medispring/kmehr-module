package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import java.nio.ByteBuffer

fun ByteBuffer.toDataBuffer(): DataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(array())

fun Flow<ByteBuffer>.asDataBuffer() = this.map { it.toDataBuffer() }
