package ru.pixnews.wasm.host.wasi.preview1.type

import ru.pixnews.wasm.host.WasmValueType


/**
 * Type names used by low-level WASI interfaces.
 * https://raw.githubusercontent.com/WebAssembly/WASI/main/legacy/preview1/witx/typenames.witx
 */
public object WasiValueTypes {
    val U8: WasmValueType = WasmValueType.I32
    val U16: WasmValueType = WasmValueType.I32
    val S32: WasmValueType = WasmValueType.I32
    val U32: WasmValueType = WasmValueType.I32
    val S64: WasmValueType = WasmValueType.I64
    val U64: WasmValueType = WasmValueType.I64
    val Handle: WasmValueType = WasmValueType.I32
}