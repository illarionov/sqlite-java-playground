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

typealias Sqlite3TraceCallback = (db: WasmPtr<Sqlite3Db>, statement: String) -> Unit

typealias Sqlite3Profile = (db: WasmPtr<Sqlite3Db>, statement: String, time: Long) -> Unit