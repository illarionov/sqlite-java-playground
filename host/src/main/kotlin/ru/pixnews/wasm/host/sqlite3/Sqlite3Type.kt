package ru.pixnews.wasm.host.sqlite3

import ru.pixnews.wasm.host.WasmPtr

typealias Sqlite3Db = Void

typealias Sqlite3ExecCallback = (
    sqliteDb: WasmPtr<Sqlite3Db>,    // *void
    columns: Int,
    pResults: WasmPtr<WasmPtr<Byte>>, // **char
    pColumnNames: WasmPtr<WasmPtr<Byte>> //**char
) -> Int
