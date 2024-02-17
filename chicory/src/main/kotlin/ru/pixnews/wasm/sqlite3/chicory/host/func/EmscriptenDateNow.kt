package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.time.Clock
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun emscriptenDateNow(
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    EmscriptenDateNow(clock),
    moduleName,
    "emscripten_date_now",
    listOf(),
    listOf(ValueType.F64),
)

private class EmscriptenDateNow(
    private val clock: Clock,
    private val logger: Logger = Logger.getLogger(EmscriptenDateNow::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val millis = clock.millis()
        return arrayOf(Value.fromDouble(millis.toDouble()))
    }
}