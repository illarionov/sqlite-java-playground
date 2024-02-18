package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.memory.encodeToNullTerminatedByteArray

fun syscallGetcwd(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_getcwd",
    paramTypes = listOf(
        U8.pointer, // buf
        I32, // size
    ),
    returnType = Errno.wasmValueType,
    moduleName = moduleName,
    handle = Getcwd(filesystem)
)

private class Getcwd(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Getcwd::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val result = getCwd(
            instance,
            args[0].asWasmAddr(),
            args[1].asInt(),
        )
        return Value.i32(result.toLong())
    }

    private fun getCwd(
        instance: Instance,
        dst: WasmPtr,
        size: Int
    ): Int {
        logger.finest { "getCwd(dst: 0x${dst.toString(16)} size: $size)" }
        if (size == 0) return -Errno.INVAL.code

        val path = filesystem.getCwd()
        val pathBytes = path.encodeToNullTerminatedByteArray()

        if (size < pathBytes.size) {
            return -Errno.RANGE.code
        }
        instance.memory().write(dst, pathBytes)

        return pathBytes.size
    }
}