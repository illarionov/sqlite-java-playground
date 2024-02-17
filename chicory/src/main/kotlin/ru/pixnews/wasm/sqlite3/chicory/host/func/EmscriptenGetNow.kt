package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun emscriptenGetNow(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    EmscriptenGetNow(),
    moduleName,
    "emscripten_get_now",
    listOf(),
    listOf(ValueType.F64),
)

private class EmscriptenGetNow(
    private val logger: Logger = Logger.getLogger(EmscriptenGetNow::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val ts = System.nanoTime() / 1_000_000.0
        return arrayOf(Value.fromDouble(ts))
    }
}