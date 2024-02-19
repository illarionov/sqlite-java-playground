package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.include.sys.pack
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.memory.ChicoryMemoryImpl

fun syscallLstat64(
    memory: ChicoryMemoryImpl,
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = stat64Func(
    memory = memory,
    filesystem = filesystem,
    fieldName = "__syscall_lstat64",
    followSymlinks = false,
    moduleName = moduleName
)

fun syscallStat64(
    memory: ChicoryMemoryImpl,
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = stat64Func(
    memory = memory,
    filesystem = filesystem,
    fieldName = "__syscall_stat64",
    followSymlinks = true,
    moduleName = moduleName
)

private fun stat64Func(
    memory: ChicoryMemoryImpl,
    filesystem: FileSystem,
    fieldName: String,
    followSymlinks: Boolean = true,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        U8.pointer, // pathname
        U8.pointer, // statbuf
    ),
    returnType = Errno.wasmValueType,
    moduleName = moduleName,
    handle = Stat64(memory = memory, filesystem = filesystem, followSymlinks = followSymlinks)
)

private class Stat64(
    private val memory: ChicoryMemoryImpl,
    private val filesystem: FileSystem,
    private val followSymlinks: Boolean = false,
    private val logger: Logger = Logger.getLogger(Stat64::class.qualifiedName)
) : EmscryptenHostFunction {
    private val syscallName = if (followSymlinks) "Stat64" else "Lstat64"

    override fun apply(instance: Instance, vararg args: Value): Value {
        val result = stat64(
            instance,
            args[0].asWasmAddr(),
            args[1].asWasmAddr(),
        )
        return Value.i32(result.toLong())
    }

    private fun stat64(
        instance: Instance,
        pathnamePtr: WasmPtr,
        dst: WasmPtr,
    ): Int {
        var path = ""
        try {
            path = memory.readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks
            ).also {
                logger.finest { "$syscallName($path): $it" }
            }.pack()
            instance.memory().write(dst, stat)
        } catch (e: SysException) {
            logger.finest { "$syscallName(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}