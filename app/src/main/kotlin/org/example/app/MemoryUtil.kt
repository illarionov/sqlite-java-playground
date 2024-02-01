package org.example.app

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
    var b: Byte
    var l = offset
    do {
        b = readBufferByte(l++)
        mem.write(b.toInt())
    } while (b != 0.toByte())

    return mem.toString()
}