package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.SysException
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

fun syscallUnlinkat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    Unlinkat(filesystem),
    moduleName,
    "__syscall_unlinkat",
    listOf(
        ValueType.I32, // dirfd
        ValueType.I32, // pathname
        ValueType.I32, // flags
    ),
    listOf(
        ValueType.I32 //
    ),
)

private class Unlinkat(
    private val filesystem: FileSystem,
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
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

        return arrayOf(Value.i32(-errNo.code.toLong()))
    }
}