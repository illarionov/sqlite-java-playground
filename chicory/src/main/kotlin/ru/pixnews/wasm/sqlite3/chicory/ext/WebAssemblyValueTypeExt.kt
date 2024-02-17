package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.host.WasmValueType

internal val WasmValueType.chicory: ValueType
    get() = ValueType.byId(
        requireNotNull(opcode) {
            "Can not convert Wasi type without opcode"
        }.toLong()
    )
