package ru.pixnews.wasm.host.sqlite3

import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

typealias Sqlite3Db = Void

typealias Sqlite3ExecCallback = (
    sqliteDb: WasmPtr<Sqlite3Db>,    // *void
    columns: Int,
    pColumnNames: WasmPtr<WasmPtr<Byte>>, // **char
    pResults: WasmPtr<WasmPtr<Byte>> //**char
) -> Int
