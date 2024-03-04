package org.example.app.sqlite3.callback

import org.example.app.sqlite3.callback.func.SQLITE3_COMPARATOR_CALL_FUNCTION_NAME
import org.example.app.sqlite3.callback.func.SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME
import org.example.app.sqlite3.callback.func.SQLITE3_EXEC_CB_FUNCTION_NAME
import org.example.app.sqlite3.callback.func.SQLITE3_PROGRESS_CB_FUNCTION_NAME
import org.example.app.sqlite3.callback.func.SQLITE3_TRACE_CB_FUNCTION_NAME
import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableIndex

internal class Sqlite3CallbackFunctionIndexes(
    functionMap: Map<String, IndirectFunctionTableIndex>,
) {
    val execCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CB_FUNCTION_NAME)
    val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CB_FUNCTION_NAME)
    val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CB_FUNCTION_NAME)
    val comparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_COMPARATOR_CALL_FUNCTION_NAME)
    val destroyComparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
        SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME
    )
}