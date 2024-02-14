package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.oMaskToString
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.ext.sMaskToString
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.Fcntl
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.resolveAbsolutePath
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U8
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

fun syscallOpenat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    Openat(filesystem),
    moduleName,
    "__syscall_openat",
    listOf(
        ValueType.I32, // dirfd
        U8.pointer, // pathname
        ValueType.I32, // flags
        ValueType.I32 // mode / varargs
    ),
    listOf(
        ValueType.I32 //
    ),
)


private class Openat(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Openat::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val mode = if (params.lastIndex == 3) {
            instance.memory().readI32(params[3].asWasmAddr()).asInt().toUInt()
        } else {
            0U
        }

        val fdOrErrno = openAt(
            instance = instance,
            dirfd = params[0].asInt(),
            pathnamePtr = params[1].asWasmAddr(),
            flags = params[2].asInt().toUInt(),
            mode = mode,
        )
        return arrayOf(Value.i32(fdOrErrno.toLong()))
    }

    private fun openAt(
        instance: Instance,
        dirfd: Int,
        pathnamePtr: WasmPtr,
        flags: UInt,
        mode: UInt
    ) : Int {
        val path = instance.memory().readNullTerminatedString(pathnamePtr)
        val absolutePath = filesystem.resolveAbsolutePath(dirfd, path)

        logger.finest { "openAt() dirfd: " +
                "$dirfd, " +
                "path: `$path`, " +
                "full path: `$absolutePath`, " +
                "flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}), " +
                "mode: ${Fcntl.sMaskToString(mode)}"
        }

        return try {
            filesystem.open(absolutePath, flags, mode).fd.fd
        } catch (e: SysException) {
            logger.finest { "openAt() error ${e.errNo}" }
            -e.errNo.code
        }
    }
}