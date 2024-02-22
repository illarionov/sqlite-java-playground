package ru.pixnews.wasm.host.sqlite3

import ru.pixnews.wasm.host.WasmPtr

typealias Sqlite3Db = Void

typealias Sqlite3ExecCallback = (
    sqliteDb: WasmPtr<Sqlite3Db>,    // *void
    results: List<String>, // **char
    columnNames: List<String> //**char
) -> Int
