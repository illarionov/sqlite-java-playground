package ru.pixnews.wasm.host.memory

import java.io.ByteArrayOutputStream
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

fun Memory.readNullableNullTerminatedString(offset: WasmPtr): String? {
    return if (offset != 0) {
        readNullTerminatedString(offset)
    } else {
        null
    }
}

fun Memory.readNullTerminatedString(offset: WasmPtr): String {
    check(offset != 0)
    val mem = ByteArrayOutputStream()
    var l = offset
    do {
        val b = this.readI8(l++)
        if (b == 0.toByte()) break
        mem.write(b.toInt())
    } while (true)

    return mem.toString(Charsets.UTF_8)
}

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