package ru.pixnews.wasm.sqlite3.chicory.host.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory.PAGE_SIZE
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import java.util.logging.Logger
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite3.chicory.ext.ParamTypes
import ru.pixnews.wasm.sqlite3.chicory.host.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite3.host.filesystem.SysException

fun emscriptenResizeHeap(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = HostFunction(
    EmscriptenResizeHeap(),
    moduleName,
    "emscripten_resize_heap",
    listOf(
        ValueType.I32 // requestedSize
    ),
    ParamTypes.i32,
)

private class EmscriptenResizeHeap(
    private val logger: Logger = Logger.getLogger(EmscriptenResizeHeap::class.qualifiedName)
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg params: Value): Array<Value> {
        val memory = instance.memory()
        val requestedSize = params[0].asInt().toLong()

        val newSizePages = calculateNewSizePages(
            requestedSize,
            memory.pages().toLong(),
            memory.maximumPages().toLong()
        )

        logger.finest {
            "emscripten_resize_heap($requestedSize). " +
                "Requested: ${newSizePages * PAGE_SIZE} bytes ($newSizePages pages)"
        }

        val prevPages = memory.grow((newSizePages - memory.pages()).toInt())
        if (prevPages < 0) {
            throw SysException(
                Errno.NOMEM,
                "Cannot enlarge memory, requested $newSizePages pages, but the limit is ${memory.maximumPages()} pages!"
            )
        }
        return arrayOf(Value.i32(1))
    }

    companion object {
        fun calculateNewSizePages(
            requestedSizeBytes: Long,
            memoryPages: Long,
            memoryMaxPages: Long,
        ): Long {
            check(requestedSizeBytes > memoryPages * PAGE_SIZE)

            val oldSize = memoryPages * PAGE_SIZE
            val overGrownHeapSize = minOf(
                oldSize + (oldSize / 5),
                requestedSizeBytes + 100663296L
            ).coerceAtLeast(requestedSizeBytes)
            return ((overGrownHeapSize + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtMost(memoryMaxPages)
        }
    }
}