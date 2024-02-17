package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.host.WebAssemblyValueType

public val WebAssemblyValueType.chicory: ValueType
    get() = ValueType.byId(
        requireNotNull(opcode) {
            "Can not convert Wasi type without opcode"
        }.toLong()
    )
