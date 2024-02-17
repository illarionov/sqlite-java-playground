package ru.pixnews.wasm.host

// https://webassembly.github.io/spec/core/appendix/index-types.html
@JvmInline
public value class WasmValueType(
    val opcode: Byte?
) {
    public companion object WebAssemblyTypes {
        val I32 = WasmValueType(0x7f)
        val I64 = WasmValueType(0x7e)
        val F32 = WasmValueType(0x7d)
        val F64 = WasmValueType(0x7c)
        val V128 = WasmValueType(0x7b)
        val FuncRef = WasmValueType(0x70)
        val ExternRef = WasmValueType(0x6f)
        val FunctionType = WasmValueType(0x60)
        val ResultType = WasmValueType(0x40)
    }
}

