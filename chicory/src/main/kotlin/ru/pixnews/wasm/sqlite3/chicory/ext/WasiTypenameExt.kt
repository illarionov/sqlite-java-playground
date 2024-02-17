package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.host.wasi.preview1.type.WasiTypename

internal val WasiTypename.valueType: ValueType
    get() = ValueType.byId(
        requireNotNull(webAssemblyValueType.opcode) {
            "Can not convert Wasi type without opcode"
        }.toLong()
    )