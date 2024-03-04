package ru.pixnews.wasm.host

import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32

public val POINTER: WasmValueType get() = I32

public val WasmValueType.pointer: WasmValueType
    get() = POINTER
