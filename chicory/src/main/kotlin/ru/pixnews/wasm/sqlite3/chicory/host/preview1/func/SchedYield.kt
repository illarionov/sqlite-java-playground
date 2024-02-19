package ru.pixnews.wasm.sqlite3.chicory.host.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction

fun schedYield(
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "sched_yield",
    paramTypes = listOf(),
    moduleName = moduleName,
    handle = SchedYield()
)

private class SchedYield(
    private val logger: Logger = Logger.getLogger(SchedYield::class.qualifiedName),
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        Thread.yield()
        return Errno.SUCCESS
    }
}