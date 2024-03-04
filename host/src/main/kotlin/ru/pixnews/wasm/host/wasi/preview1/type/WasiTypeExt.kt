package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.POINTER
import ru.pixnews.wasm.host.WasmValueType

public val WasiTypename.pointer: WasmValueType
    get() = POINTER
