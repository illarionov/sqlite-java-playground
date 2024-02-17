package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite3.chicory.ext.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.ext.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun syscallFchown32(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_fchown32",
    paramTypes = listOf(
        Fd.webAssemblyValueType,  // fd
        I32, // owner,
        I32, // group,
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = Fchown32(filesystem)
)

private class Fchown32(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Fchown32::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val fd = Fd(args[0].asInt())
        val owner = args[1].asInt()
        val group = args[2].asInt()
        val code = try {
            filesystem.chown(fd, owner, group)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.finest { "chown($fd, $owner, $group): Error ${e.errNo}" }
            e.errNo
        }
        return Value.i32(-code.code.toLong())
    }
}