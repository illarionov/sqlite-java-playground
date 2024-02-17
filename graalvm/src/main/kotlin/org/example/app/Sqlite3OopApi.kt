package org.example.app

import org.example.app.bindings.SqliteBindings

class Sqlite3OopApi(
    private val sqlite: SqliteBindings
) {
    fun db(
        path: String,
        flags: String
    ): Sqlite3Database {
        return Sqlite3Database(sqlite, path, flags)
    }

    public class Sqlite3Database internal constructor(
        private val sqlite: SqliteBindings,
        private val name: String,
        private val flags: String
    ) {
        fun exec(sql: String): Long? {
            TODO("Not yet implemented")
        }
        fun exec(
            sql: String,
            bind: List<Any>,
        ): Long? {
            TODO("Not yet implemented")
        }
        fun exec(
            sql: String,
            bind: Map<String, Any>,
        ): Long? {
            TODO("Not yet implemented")
        }

        fun close() {
            TODO("Not yet implemented")
        }

        public val filename: String
            get() = TODO()

    }
}