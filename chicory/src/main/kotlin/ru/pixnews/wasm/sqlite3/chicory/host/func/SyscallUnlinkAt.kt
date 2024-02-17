package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.WebAssemblyValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.ext.EmscryptenHostFunction
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun syscallUnlinkat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_unlinkat",
    paramTypes = listOf(
        I32, // dirfd
        I32, // pathname
        I32, // flags
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = Unlinkat(filesystem)
)

private class Unlinkat(
    private val filesystem: FileSystem,
) : EmscryptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val dirfd = args[0].asInt()
        val pathnamePtr = args[1].asWasmAddr()
        val flags = args[2].asInt().toUInt()

        val errNo = try {
            val path = instance.memory().readNullTerminatedString(pathnamePtr)
            filesystem.unlinkAt(dirfd, path, flags)
            Errno.SUCCESS
        } catch (e: SysException) {
            e.errNo
        }

        return Value.i32(-errNo.code.toLong())
    }
}