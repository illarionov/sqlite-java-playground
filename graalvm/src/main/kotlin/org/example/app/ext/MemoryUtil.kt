package org.example.app.ext

import java.io.ByteArrayOutputStream
import org.graalvm.polyglot.Value

fun Value.readNullTerminatedString(offsetValue: Value): String? {
    return if (!offsetValue.isNull) {
        this.readNullTerminatedString(offsetValue.asInt().toLong())
    } else {
        null
    }
}

fun Value.readNullTerminatedString(offset: Long): String {
    val mem = ByteArrayOutputStream()
    var l = offset
    do {
        val b = this.readBufferByte(l++)
        if (b == 0.toByte()) break
        mem.write(b.toInt())
    } while (true)

    return mem.toString()
}