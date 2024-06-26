package org.example.app.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.host.BaseWasmNode
import org.example.app.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.filesystem.SysException
import ru.pixnews.wasm.host.wasi.preview1.type.Errno

class EmscriptenResizeHeap(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "emscripten_resize_heap",
    private val logger: Logger = Logger.getLogger(EmscriptenResizeHeap::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        return emscriptenResizeheap((frame.arguments[0] as Int).toLong())
    }

    @CompilerDirectives.TruffleBoundary
    private fun emscriptenResizeheap(
        requestedSize: Long
    ) : Int = try {
        val currentPages = memory.memory.size()
        val declaredMaxPages = memory.memory.declaredMaxSize()
        val newSizePages = calculateNewSizePages(requestedSize, currentPages, declaredMaxPages)

        logger.finest {
            "emscripten_resize_heap($requestedSize). " +
                    "Requested: ${newSizePages * PAGE_SIZE} bytes ($newSizePages pages)"
        }

        val memoryAdded = memory.memory.grow(newSizePages - currentPages)
        if (!memoryAdded) {
            throw SysException(
                Errno.NOMEM,
                "Cannot enlarge memory, requested $newSizePages pages, but the limit is ${memory.memory.declaredMaxSize()} pages!"
            )
        }
        1
    } catch (e: SysException) {
        -e.errNo.code
    }

    companion object {
        const val PAGE_SIZE = 65536

        // XXX: copy of chicory version
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