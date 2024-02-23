package org.example.app

import java.util.logging.Logger
import org.example.app.bindings.SqliteBindings
import org.example.app.sqlite3.Sqlite3CApi
import org.example.app.sqlite3.Sqlite3OopApi
import org.example.app.sqlite3.callback.Sqlite3CallbackStore

class SqliteBasicDemo1(
    private val sqlite: SqliteBindings,
    callbackStore: Sqlite3CallbackStore,
    private val log: Logger = Logger.getLogger(SqliteBasicDemo1::class.simpleName)
) {
    private val cApi = Sqlite3CApi(sqlite, callbackStore)
    private val oopApi = Sqlite3OopApi(sqlite)

    fun run() {
        val version = cApi.version
        log.info { "Sqlite3 version: $version" }

        val enumJson = sqlite.sqlite3WasmEnumJson
        log.info { "Internal structures: $enumJson" }

        val dbRes = cApi.sqlite3Open("mydb.db"
            //, TODO: p
        )

        val db = oopApi.db("/mydb.sqlite3", "ct")
        log.info { "transient db: ${db.filename}" }

        /*
        Never(!) rely on garbage collection to clean up DBs and
        (especially) prepared statements. Always wrap their lifetimes
        in a try/finally construct, as demonstrated below. By and
        large, client code can entirely avoid lifetime-related
        complications of prepared statement objects by using the
        DB.exec() method for SQL execution.
         */
        try {
            log.info("Create a table...");
            db.exec("CREATE TABLE IF NOT EXISTS t(a,b)")

            log.info { "Insert some data using exec()..." }
            (20..25).forEach { i ->
                db.exec(
                    sql = "insert into t(a,b) values (?,?)",
                    bind = listOf(i, i*2)
                );
                db.exec(
                    sql =  "insert into t(a,b) values (\$a,\$b)",
                    // bind by parameter name...
                    bind = mapOf(
                        "a" to i * 10,
                        "b" to i * 20
                    )
                )
                // …
            }
        } finally {
            db.close()
        }
    }
}