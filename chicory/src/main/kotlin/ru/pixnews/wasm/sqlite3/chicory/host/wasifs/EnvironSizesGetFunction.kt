package ru.pixnews.wasm.sqlite3.chicory.host.wasifs

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes

/**
 * https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md#-wasi_snapshot_preview1
 *
 * environ_sizes_get() -> Result<(size, size), errno>
 *
 */
fun environSizeGetHostFunction(
    moduleName: String = "wasi_snapshot_preview1",
    envProvider: () -> Map<String, String> = System::getenv
) : HostFunction = HostFunction(
    EnvironSizesGetFunction(envProvider),
    moduleName,
    "environ_sizes_get",
    ParamTypes.i32i32,
    ParamTypes.i32,
)

private class EnvironSizesGetFunction(
    private val envProvider: () -> Map<String, String> = System::getenv
) : WasmFunctionHandle {
    override fun apply(
        instance: Instance,
        vararg params: Value
    ): Array<Value> {
        val result = environSizesGet(
            instance,
            params[0].asInt(),
            params[1].asInt()
        )

        return arrayOf(Value.i32(result.toLong()))
    }

    private fun environSizesGet(
        instance: Instance,
        environCountAddr: Int,
        envioronSizeAddr: Int
    ): Int {
        //val env = env
        instance.memory().writeI32(environCountAddr, 0)
        instance.memory().writeI32(envioronSizeAddr, 0)
        return 0
    }
}