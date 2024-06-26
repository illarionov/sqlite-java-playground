package ru.pixnews.wasm.sqlite3.chicory.host.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WasiHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction

fun argsGet(
    argsProvider: () -> List<String> = ::emptyList,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "args_get",
    paramTypes = listOf(I32, I32),
    moduleName = moduleName,
    handle = ArgsGet(argsProvider)
)

private class ArgsGet(
    argsProvider: () -> List<String>,
    private val logger: Logger = Logger.getLogger(ArgsGet::class.qualifiedName),
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        TODO("Not yet implemented")
    }
}