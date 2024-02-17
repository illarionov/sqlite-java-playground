package ru.pixnews.wasm.host

// https://webassembly.github.io/spec/core/appendix/index-types.html
@JvmInline
public value class WebAssemblyValueType(
    val opcode: Byte?
) {
    public companion object WebAssemblyTypes {
        val I32 = WebAssemblyValueType(0x7f)
        val I64 = WebAssemblyValueType(0x7e)
        val F32 = WebAssemblyValueType(0x7d)
        val F64 = WebAssemblyValueType(0x7c)
        val V128 = WebAssemblyValueType(0x7b)
        val FuncRef = WebAssemblyValueType(0x70)
        val ExternRef = WebAssemblyValueType(0x6f)
        val FunctionType = WebAssemblyValueType(0x60)
        val ResultType = WebAssemblyValueType(0x40)
    }
}

