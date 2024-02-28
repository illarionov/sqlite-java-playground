package ru.pixnews.wasm.host.sqlite3

typealias Sqlite3Db = Void
typealias Sqlite3Statement = Void

typealias Sqlite3ExecCallback = (
    results: List<String>, // **char
    columnNames: List<String> //**char
) -> Int
