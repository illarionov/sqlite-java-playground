package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType

public val WasiTypename.pointer: WasmValueType
    get() = WasmValueType.I32

public val WasmValueType.pointer: WasmValueType
    get() = WasmValueType.I32
