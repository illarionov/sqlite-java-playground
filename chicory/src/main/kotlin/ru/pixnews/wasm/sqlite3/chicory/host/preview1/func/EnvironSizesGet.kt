package ru.pixnews.wasm.sqlite3.chicory.host.preview1.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import ru.pixnews.wasm.host.memory.encodedNullTerminatedStringLength
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.Size
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.pointer
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite3.chicory.host.preview1.wasiHostFunction

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
): HostFunction = wasiHostFunction(
    funcName = "environ_sizes_get",
    paramTypes = listOf(
        Size.pointer, // *environ_count
        Size.pointer, // *environ_buf_size
    ),
    moduleName = moduleName,
) { instance, params ->
    environSizesGet(
        envProvider,
        instance,
        params[0].asWasmAddr(),
        params[1].asWasmAddr()
    )
}

private fun environSizesGet(
    envProvider: () -> Map<String, String>,
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

// TODO: sanitize `=`?
internal fun Map.Entry<String, String>.encodeEnvToWasi() = "${key}=${value}"