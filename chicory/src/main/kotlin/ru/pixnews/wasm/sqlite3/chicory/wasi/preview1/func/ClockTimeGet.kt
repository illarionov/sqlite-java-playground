package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.time.Clock
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1

fun clockTimeGet(
    clock: Clock = Clock.systemDefaultZone(),
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    ClockTimeGet(clock),
    moduleName,
    "clock_time_get",
    listOf(ValueType.I32, ValueType.I64, ValueType.I32),
    ParamTypes.i32,
)

private class ClockTimeGet(
    clock: Clock = Clock.systemDefaultZone(),
    private val logger: Logger = Logger.getLogger(ClockTimeGet::class.qualifiedName),
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        TODO("Not yet implemented")
    }
}