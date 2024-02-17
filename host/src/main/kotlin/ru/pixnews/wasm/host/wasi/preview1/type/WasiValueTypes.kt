package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WebAssemblyValueType


/**
 * Type names used by low-level WASI interfaces.
 * https://raw.githubusercontent.com/WebAssembly/WASI/main/legacy/preview1/witx/typenames.witx
 */
public object WasiValueTypes {
    val U8: WebAssemblyValueType = WebAssemblyValueType.I32
    val U16: WebAssemblyValueType = WebAssemblyValueType.I32
    val S32: WebAssemblyValueType = WebAssemblyValueType.I32
    val U32: WebAssemblyValueType = WebAssemblyValueType.I32
    val S64: WebAssemblyValueType = WebAssemblyValueType.I64
    val U64: WebAssemblyValueType = WebAssemblyValueType.I64
    val Handle: WebAssemblyValueType = WebAssemblyValueType.I32
}