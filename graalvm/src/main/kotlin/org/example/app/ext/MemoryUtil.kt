package org.example.app.ext

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.readNullTerminatedString
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

internal fun Value.asWasmAddr(): WasmPtr = asInt()

// TODO: ???
fun WasmPtr.isNull(): Boolean = this == 0

fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (!offsetValue.isNull) {
        this.readNullTerminatedString(offsetValue.asWasmAddr())
    } else {
        null
    }
}
