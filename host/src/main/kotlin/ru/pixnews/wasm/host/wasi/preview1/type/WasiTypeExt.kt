package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType

public val WasiTypename.pointer: WebAssemblyValueType
    get() = WebAssemblyValueType.I32

public val WebAssemblyValueType.pointer: WebAssemblyValueType
    get() = WebAssemblyValueType.I32
