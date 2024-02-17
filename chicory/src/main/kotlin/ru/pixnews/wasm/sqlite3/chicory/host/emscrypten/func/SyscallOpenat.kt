package ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import java.nio.file.Path
import java.util.logging.Logger
import ru.pixnews.wasm.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.host.emscrypten.emscriptenEnvHostFunction
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.include.Fcntl
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.filesystem.resolveAbsolutePath
import ru.pixnews.wasm.host.include.oMaskToString
import ru.pixnews.wasm.host.include.sMaskToString

fun syscallOpenat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_openat",
    paramTypes = listOf(
        I32, // dirfd
        U8.pointer, // pathname
        I32, // flags
        I32 // mode / varargs
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = Openat(filesystem)
)

private class Openat(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Openat::class.qualifiedName)
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val mode = if (args.lastIndex == 3) {
            instance.memory().readI32(args[3].asWasmAddr()).asInt().toUInt()
        } else {
            0U
        }

        val fdOrErrno = openAt(
            instance = instance,
            dirfd = args[0].asInt(),
            pathnamePtr = args[1].asWasmAddr(),
            flags = args[2].asInt().toUInt(),
            mode = mode,
        )
        return Value.i32(fdOrErrno.toLong())
    }

    private fun openAt(
        instance: Instance,
        dirfd: Int,
        pathnamePtr: WasmPtr,
        flags: UInt,
        mode: UInt
    ): Int {
        val path = instance.memory().readNullTerminatedString(pathnamePtr)
        val absolutePath = filesystem.resolveAbsolutePath(dirfd, path)

        return try {
            val fd = filesystem.open(absolutePath, flags, mode).fd
            logger.finest { formatCallString(dirfd, path, absolutePath, flags, mode, fd) }
            fd.fd
        } catch (e: SysException) {
            logger.finest {
                formatCallString(dirfd, path, absolutePath, flags, mode, null) +
                        "openAt() error ${e.errNo}"
            }
            -e.errNo.code
        }
    }

    private fun formatCallString(
        dirfd: Int,
        path: String,
        absolutePath: Path,
        flags: UInt,
        mode: UInt,
        fd: Fd?
    ): String = "openAt() dirfd: " +
            "$dirfd, " +
            "path: `$path`, " +
            "full path: `$absolutePath`, " +
            "flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}), " +
            "mode: ${Fcntl.sMaskToString(mode)}" +
            if (fd != null) ": $fd" else ""
}