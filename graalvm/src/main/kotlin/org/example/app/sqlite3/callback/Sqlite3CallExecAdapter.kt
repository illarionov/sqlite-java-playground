package org.example.app.sqlite3.callback

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.example.app.sqlite3.callback.Sqlite3CallbackStore.Sqlite3ExecCallbackId
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.host.memory.readPtr
import ru.pixnews.wasm.host.plus

const val SQLITE3_EXEC_CB_FUNCTION_NAME = "sqlite3_exec_cb"

class Sqlite3CallExecAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3CallExecAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return callDelegate(
            args[0] as Int,
            args[1] as Int,
            args.asWasmPtr(2),
            args.asWasmPtr(3),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun callDelegate(
        arg1: Int,
        columns: Int,
        pResults: WasmPtr<WasmPtr<Byte>>,
        pColumnNames: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.finest() { "cb() arg1: $arg1 columns: $columns names: $pColumnNames results: $pResults" }
        val delegateId = Sqlite3ExecCallbackId(arg1)
        val delegate = callbackStore.sqlite3ExecCallbacks[delegateId] ?: error("Callback $delegateId not registered")

        val columnNames = (0 until columns).map { columnNo ->
            val ptr = memory.readPtr<Byte>(pColumnNames + (columnNo * WASM_SIZEOF_PTR.toInt()))
            memory.readNullTerminatedString(ptr)
        }

        val results =  (0 until columns).map { columnNo ->
            val ptr = memory.readPtr<Byte>(pResults + (columnNo * WASM_SIZEOF_PTR.toInt()))
            memory.readNullTerminatedString(ptr)
        }
        return delegate(columnNames, results)
    }
}