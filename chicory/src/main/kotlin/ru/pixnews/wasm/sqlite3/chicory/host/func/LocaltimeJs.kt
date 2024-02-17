package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.time.Clock
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME

fun localtimeJs(
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    LocaltimeJs(clock),
    moduleName,
    "_localtime_js",
    listOf(ValueType.I64, ValueType.I32),
    listOf(),
)

private class LocaltimeJs(
    private val clock: Clock,
    private val logger: Logger = Logger.getLogger(LocaltimeJs::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}