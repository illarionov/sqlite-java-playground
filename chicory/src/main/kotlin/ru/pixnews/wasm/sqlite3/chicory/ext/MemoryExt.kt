package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.memory.Memory
import ru.pixnews.wasm.host.memory.readNullableNullTerminatedString

fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (offsetValue.asExtRef() != Value.REF_NULL_VALUE) {
        this.readNullableNullTerminatedString(offsetValue.asWasmAddr())
    } else {
        null
    }
}
