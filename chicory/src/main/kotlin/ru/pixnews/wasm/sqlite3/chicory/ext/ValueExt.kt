package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.host.WasmPtr

internal fun <P: Any?> Value.asWasmAddr(): WasmPtr<P> = WasmPtr(asInt())

internal fun WasmPtr<*>.asValue(): Value = Value.i32(this.addr.toLong())

internal fun Value?.isNull(): Boolean {
    return when (this?.type()) {
        null -> true
        ValueType.F64,
        ValueType.F32,
        ValueType.I64,
        ValueType.I32 -> this.asLong() == -1L

        ValueType.V128 -> error("Not implemented")
        ValueType.FuncRef -> this.asFuncRef() == REF_NULL_VALUE
        ValueType.ExternRef -> this.asExtRef() == REF_NULL_VALUE
    }
}

internal val SQLITE3_NULLVAL = Value.i32(0)
