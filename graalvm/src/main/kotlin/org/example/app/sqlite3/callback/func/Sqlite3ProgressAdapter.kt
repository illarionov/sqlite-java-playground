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
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3ProgressCallback

const val SQLITE3_PROGRESS_CB_FUNCTION_NAME = "sqlite3_progress_cb"

class Sqlite3ProgressAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3ProgressAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return invokeProgressCallback(
            args.asWasmPtr(0),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeProgressCallback(
        contextPointer: WasmPtr<Sqlite3Db>,
    ): Int {
        logger.finest { "invokeProgressCallback() db: $contextPointer" }
        val delegate: Sqlite3ProgressCallback = callbackStore.sqlite3ProgressCallbacks[contextPointer]
            ?: error("Callback $contextPointer not registered")

        return delegate.invoke(contextPointer)
    }
}