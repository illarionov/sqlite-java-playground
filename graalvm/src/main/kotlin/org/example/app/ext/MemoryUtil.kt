package org.example.app.ext

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.readNullTerminatedString
import ru.pixnews.wasm.host.WasmPtr

internal fun <P: Any?> Value.asWasmAddr(): WasmPtr<P> = WasmPtr(asInt())

internal fun <P: Any?> Array<Any>.asWasmPtr(idx: Int): WasmPtr<P> = WasmPtr(this[idx] as Int)

fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (!offsetValue.isNull) {
        this.readNullTerminatedString(offsetValue.asWasmAddr())
    } else {
        null
    }
}
