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
    var l = offset
    do {
        val b = this.read(l++)
        if (b == 0.toByte()) break
        mem.write(b.toInt())
    } while (true)

    return mem.toString()
}