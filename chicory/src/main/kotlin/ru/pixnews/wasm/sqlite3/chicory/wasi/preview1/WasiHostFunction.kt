package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.WebAssemblyValueType
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.ext.chicory
import ru.pixnews.wasm.sqlite3.chicory.ext.valueType

internal const val WASI_SNAPSHOT_PREVIEW1 = "wasi_snapshot_preview1"

internal fun wasiHostFunction(
    funcName: String,
    paramTypes : List<WebAssemblyValueType>,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
    handle: WasiHostFunction,
) : HostFunction = HostFunction(
    WasiHostFunctionAdapter(handle),
    moduleName,
    funcName,
    paramTypes.map(WebAssemblyValueType::chicory),
    listOf(Errno.valueType)
)

internal fun interface WasiHostFunction {
    fun apply(instance: Instance, vararg args: Value): Errno
}

private class WasiHostFunctionAdapter(
    private val delegate: WasiHostFunction
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val result = delegate.apply(instance, args = args)
        return arrayOf(Value.i32(result.code.toLong()))
    }
}
