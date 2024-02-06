package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType.I32
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.encodeToNullTerminatedByteArray
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.U8
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

fun syscallGetcwd(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    SyscallGetcwd(filesystem),
    moduleName,
    "__syscall_getcwd",
    listOf(
        U8.pointer, // buf
        I32, // size
    ),
    listOf(
        Errno.tag
    ),
)

private class SyscallGetcwd(
    private val filesystem: FileSystem,
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val result = getCwd(
            instance,
            params[0].asWasmAddr(),
            params[1].asInt(),
        )
        return arrayOf(Value.i32(result.toLong()))
    }

    private fun getCwd(
        instance: Instance,
        dst: WasmAddr,
        size: Int
    ): Int {
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