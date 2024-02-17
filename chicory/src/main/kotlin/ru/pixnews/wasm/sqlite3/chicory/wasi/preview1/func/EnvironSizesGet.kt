package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.ext.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.ext.WasmPtr
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Size
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.pointer

/**
 * Return environment variable data sizes.
 *
 * https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/witx/wasi_snapshot_preview1.witx
 *
 * (@interface func (export "environ_sizes_get")
 *   ;;; Returns the number of environment variable arguments and the size of the
 *   ;;; environment variable data.
 *   (result $error (expected (tuple $size $size) (error $errno)))
 * )
 *
 */
fun environSizesGet(
    envProvider: () -> Map<String, String> = System::getenv,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
) : HostFunction = HostFunction(
    EnvironSizesGet(envProvider),
    moduleName,
    "environ_sizes_get",
    listOf(
        Size.pointer, // *environ_count
        Size.pointer, // *environ_buf_size
    ),
    listOf(Errno.valueType),
)

private class EnvironSizesGet(
    private val envProvider: () -> Map<String, String>
) : WasmFunctionHandle {
    override fun apply(
        instance: Instance,
        vararg params: Value
    ): Array<Value> {
        val result = environSizesGet(
            instance,
            params[0].asWasmAddr(),
            params[1].asWasmAddr()
        )

        return arrayOf(result.value)
    }

    private fun environSizesGet(
        instance: Instance,
        environCountAddr: WasmPtr,
        environSizeAddr: WasmPtr,
    ): Errno {
        val env = envProvider()
        val count = env.size
        val dataLength = env.entries.sumOf { it.encodeEnvToWasi().encodedNullTerminatedStringLength() }

        instance.memory().writeI32(environCountAddr, count)
        instance.memory().writeI32(environSizeAddr, dataLength)
        return Errno.SUCCESS
    }
}

// TODO: sanitize `=`?
internal fun Map.Entry<String, String>.encodeEnvToWasi() = "${key}=${value}"