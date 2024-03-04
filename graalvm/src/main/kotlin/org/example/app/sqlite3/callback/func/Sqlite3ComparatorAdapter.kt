package org.example.app.sqlite3.callback.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.example.app.sqlite3.callback.Sqlite3CallbackStore
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.sqlite3.Sqlite3ComparatorCallback

const val SQLITE3_COMPARATOR_CALL_FUNCTION_NAME = "sqlite3_comparator_call_cb"

class Sqlite3ComparatorAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3ProgressAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return invokeComparator(
            Sqlite3CallbackStore.Sqlite3ComparatorId(args[0] as Int),
            args[1] as Int,
            args.asWasmPtr(2),
            args[3] as Int,
            args.asWasmPtr(4),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeComparator(
        comparatorId: Sqlite3CallbackStore.Sqlite3ComparatorId,
        str1Size: Int,
        str1: WasmPtr<Byte>,
        str2Size: Int,
        str2: WasmPtr<Byte>,
    ): Int {
        logger.finest { "invokeComparator() db: $comparatorId" }
        val delegate: Sqlite3ComparatorCallback = callbackStore.sqlite3Comparators[comparatorId]
            ?: error("Comparator $comparatorId not registered")

        val str1Bytes = memory.readBytes(str1, str1Size)
        val str2Bytes = memory.readBytes(str2, str2Size)

        return delegate.invoke(String(str1Bytes), String(str2Bytes))
    }
}