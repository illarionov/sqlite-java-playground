package ru.pixnews.wasm.host.sqlite3

import ru.pixnews.wasm.host.WasmPtr

typealias Sqlite3Db = Void
typealias Sqlite3Statement = Void

typealias Sqlite3ExecCallback = (
    results: List<String>, // **char
    columnNames: List<String> //**char
) -> Int

typealias Sqlite3ComparatorCallback = (
    a: String, b: String
) -> Int

typealias Sqlite3ComparatorCallbackRaw = (
    aLength: Int,
    aPtr: WasmPtr<Byte>,
    bLength: Int,
    bPtr: WasmPtr<Byte>,
) -> Int

typealias Sqlite3TraceCallback = (trace: Sqlite3Trace) -> Unit

typealias Sqlite3ProgressCallback = (db: WasmPtr<Sqlite3Db>) -> Int

public sealed class Sqlite3Trace {
    class TraceStmt(
        val db: WasmPtr<Sqlite3Db>,
        val statement: WasmPtr<Sqlite3Statement>,
        val unexpandedSql: String?,
    ) : Sqlite3Trace()
    class TraceProfile(
        val db: WasmPtr<Sqlite3Db>,
        val statement: WasmPtr<Sqlite3Statement>,
        val timeMs: Long
    ): Sqlite3Trace()
    class TraceRow(
        val db: WasmPtr<Sqlite3Db>,
        val statement: WasmPtr<Sqlite3Statement>,
    ): Sqlite3Trace()
    class TraceClose(
        val db: WasmPtr<Sqlite3Db>,
    ): Sqlite3Trace()
}