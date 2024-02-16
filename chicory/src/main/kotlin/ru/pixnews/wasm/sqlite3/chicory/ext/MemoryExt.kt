package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.Value
import java.io.ByteArrayOutputStream

fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (offsetValue.asExtRef() != Value.REF_NULL_VALUE) {
        this.readNullableNullTerminatedString(offsetValue.asWasmAddr())
    } else {
        null
    }
}

fun Memory.readNullableNullTerminatedString(offset: WasmPtr): String? {
    return if (offset != 0) {
        readNullTerminatedString(offset)
    } else {
        null
    }
}

fun Memory.readNullTerminatedString(offset: WasmPtr): String {
    val mem = ByteArrayOutputStream()
    var l = offset
    do {
        val b = this.read(l++)
        if (b == 0.toByte()) break
        mem.write(b.toInt())
    } while (true)

    return mem.toString(Charsets.UTF_8)
}

fun Memory.readAddr(offset: WasmPtr): Value = readI32(offset)

fun Memory.writeNullTerminatedString(
    offset: WasmPtr,
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