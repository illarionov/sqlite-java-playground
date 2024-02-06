package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.Value
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (offsetValue.asExtRef() != Value.REF_NULL_VALUE) {
        this.readNullTerminatedString(offsetValue.asWasmAddr())
    } else {
        null
    }
}

fun Memory.readNullTerminatedString(offset: WasmAddr): String {
    val mem = ByteArrayOutputStream()
    var l = offset
    do {
        val b = this.read(l++)
        if (b == 0.toByte()) break
        mem.write(b.toInt())
    } while (true)

    return mem.toString()
}

fun Memory.readAddr(offset: WasmAddr): Value = readI32(offset)

fun Memory.writeNullTerminatedString(
    offset: WasmAddr,
    value: String,
): Int {
    val encoded = value.encodeToByteArray()
    write(offset, encoded)
    writeByte(offset + encoded.size, 0)
    return encoded.size + 1
}

fun String.encodeToNullTerminatedByteArray() : ByteArray {
    val os = ByteArrayOutputStream(this.length)
    os.writeBytes(this.encodeToByteArray())
    os.write(0)
    return os.toByteArray()
}

fun String.encodedStringLength(): Int = this.encodeToByteArray().size

fun String.encodedNullTerminatedStringLength(): Int = this.encodeToByteArray().size + 1