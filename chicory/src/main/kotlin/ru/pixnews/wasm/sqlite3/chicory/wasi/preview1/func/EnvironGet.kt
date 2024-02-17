package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WasiValueTypes.U8
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.chicory
import ru.pixnews.wasm.sqlite3.chicory.ext.valueType
import ru.pixnews.wasm.sqlite3.chicory.ext.writeNullTerminatedString

/**
 * Read environment variable data.
 * The sizes of the buffers should match that returned by `environ_sizes_get`.
 * Key/value pairs are expected to be joined with `=`s, and terminated with `\0`s.
 *
 * (@interface func (export "environ_get")
 *     (param $environ (@witx pointer (@witx pointer u8)))
 *     (param $environ_buf (@witx pointer u8))
 *     (result $error (expected (error $errno)))
 *   )
 */
fun environGet(
    envProvider: () -> Map<String, String> = System::getenv,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = HostFunction(
    EnvironGet(envProvider),
    moduleName,
    "environ_get",
    listOf(
        U8.pointer.chicory, // **environ
        U8.pointer.chicory, // *environ_buf
    ),
    listOf(Errno.valueType),
)

private class EnvironGet(
    private val envProvider: () -> Map<String, String> = System::getenv
) : WasmFunctionHandle {
    override fun apply(
        instance: Instance,
        vararg params: Value
    ): Array<Value> {
        val result = environGet(
            instance.memory(),
            params[0].asWasmAddr(),
            params[1].asWasmAddr()
        )

        return arrayOf(Value.i32(result.code.toLong()))
    }

    private fun environGet(
        memory: Memory,
        environPAddr: WasmPtr,
        environBufAddr: WasmPtr,
    ): Errno {
        var pp = environPAddr
        var bufP = environBufAddr

        envProvider()
            .entries
            .map(Map.Entry<String, String>::encodeEnvToWasi)
            .forEach { envString ->
                memory.writeI32(pp, bufP)
                pp += 4
                bufP += memory.writeNullTerminatedString(bufP, envString)
            }

        return Errno.SUCCESS
    }
}
