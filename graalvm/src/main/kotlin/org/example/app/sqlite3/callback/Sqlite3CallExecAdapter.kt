package org.example.app.sqlite3.callback

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import org.example.app.ext.asWasmPtr
import org.example.app.host.BaseWasmNode
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.memory.readPtr
import ru.pixnews.wasm.host.plus
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback

class Sqlite3CallExecAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val delegate: Sqlite3ExecCallback,
    functionName: String = "sqlite3Callback",
    private val logger: Logger = Logger.getLogger(Sqlite3CallExecAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return callDelegate(
            args.asWasmPtr(0),
            args[1] as Int,
            args.asWasmPtr(2),
            args.asWasmPtr(3),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun callDelegate(
        sqliteDb: WasmPtr<Sqlite3Db>,
        columns: Int,
        pResults: WasmPtr<WasmPtr<Byte>>,
        pColumnNames: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.finest() { "cb() db: $sqliteDb columns: $columns names: $pColumnNames results: $pResults" }
        val columnNames = (0 until columns).map { columnNo ->
            val ptr = memory.readPtr<Byte>(pColumnNames + (columnNo * WasmPtr.WASM_SIZEOF_PTR.toInt()))
            memory.readNullTerminatedString(ptr)
        }

        val results =  (0 until columns).map { columnNo ->
            val ptr = memory.readPtr<Byte>(pResults + (columnNo * WasmPtr.WASM_SIZEOF_PTR.toInt()))
            memory.readNullTerminatedString(ptr)
        }
        return delegate(sqliteDb, columnNames, results)
    }
}