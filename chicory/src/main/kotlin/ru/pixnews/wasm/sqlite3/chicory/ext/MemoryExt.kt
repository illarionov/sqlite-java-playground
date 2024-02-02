package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.Value
import java.io.ByteArrayOutputStream

fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (offsetValue.asExtRef() != Value.REF_NULL_VALUE) {
        this.readNullTerminatedString(offsetValue.asInt())
    } else {
        null
    }
}

fun Memory.readNullTerminatedString(offset: Int): String {
    val mem = ByteArrayOutputStream()
    var b: Byte
    var l = offset
    do {
        b = this.read(l++)
        mem.write(b.toInt())
    } while (b != 0.toByte())

    return mem.toString()
}