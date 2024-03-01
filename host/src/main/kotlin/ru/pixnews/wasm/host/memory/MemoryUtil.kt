package ru.pixnews.wasm.host.memory

import java.io.ByteArrayOutputStream
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.isSqlite3Null

fun Memory.readNullableNullTerminatedString(offset: WasmPtr<Byte>): String? {
    return if (!offset.isSqlite3Null()) {
        readNullTerminatedString(offset)
    } else {
        null
    }
}

fun Memory.readNullTerminatedString(offset: WasmPtr<Byte>): String {
    check(offset.addr != 0)
    val mem = ByteArrayOutputStream()
    var l = offset.addr
    do {
        val b = this.readI8(WasmPtr<Unit>(l))
        l += 1
        if (b == 0.toByte()) break
        mem.write(b.toInt())
    } while (true)

    return mem.toString("UTF-8")
}

fun Memory.writeNullTerminatedString(
    offset: WasmPtr<*>,
    value: String,
): Int {
    val encoded = value.encodeToByteArray()
    write(offset, encoded)
    writeByte(WasmPtr<Unit>(offset.addr + encoded.size), 0)
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