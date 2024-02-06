package ru.pixnews.wasm.sqlite3.chicory.ext

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE
import com.dylibso.chicory.wasm.types.ValueType

internal fun Value.asWasmAddr(): WasmAddr = asInt()

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