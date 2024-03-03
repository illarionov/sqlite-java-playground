package org.example.app.sqlite3.callback

import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableIndex

class Sqlite3CallbackFunctionIndexes(
    functionMap: Map<String, IndirectFunctionTableIndex>,
) {
    val execCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CB_FUNCTION_NAME)
    val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CB_FUNCTION_NAME)
    val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CB_FUNCTION_NAME)
}