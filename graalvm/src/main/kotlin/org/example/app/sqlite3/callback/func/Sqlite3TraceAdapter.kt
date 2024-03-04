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
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode.Companion.SQLITE_TRACE_STMT
import ru.pixnews.sqlite3.wasm.util.contains
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3Statement
import ru.pixnews.wasm.host.sqlite3.Sqlite3Trace

const val SQLITE3_TRACE_CB_FUNCTION_NAME = "sqlite3_trace_cb"

internal class Sqlite3TraceAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3TraceAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return invokeTraceCallback(
            Sqlite3TraceEventCode(args[0] as Int),
            args.asWasmPtr(1),
            args.asWasmPtr(2),
            args[3] as Long,
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeTraceCallback(
        flags: Sqlite3TraceEventCode,
        contextPointer: WasmPtr<Sqlite3Db>,
        arg1: WasmPtr<Nothing>,
        arg2: Long,
    ): Int {
        logger.finest { "invokeTraceCallback() flags: $flags db: $contextPointer arg1: $${arg1} arg3: $arg2" }
        val delegate: (trace: Sqlite3Trace) -> Unit = callbackStore.sqlite3TraceCallbacks[contextPointer] ?: error("Callback $contextPointer not registered")

        if (flags.contains(SQLITE_TRACE_STMT)) {
            val traceInfo = Sqlite3Trace.TraceStmt(
                db = contextPointer,
                statement = arg1 as WasmPtr<Sqlite3Statement>,
                unexpandedSql = memory.readNullTerminatedString(arg2 as WasmPtr<Byte>)
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_PROFILE)) {
            val traceInfo = Sqlite3Trace.TraceProfile(
                db = contextPointer,
                statement = arg1 as WasmPtr<Sqlite3Statement>,
                timeMs = arg2
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_ROW)) {
            val traceInfo = Sqlite3Trace.TraceRow(
                db = contextPointer,
                statement = arg1 as WasmPtr<Sqlite3Statement>,
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_CLOSE)) {
            val traceInfo = Sqlite3Trace.TraceClose(
                db = arg1 as WasmPtr<Sqlite3Db>,
            )
            delegate.invoke(traceInfo)
        }

        return 0
    }
}