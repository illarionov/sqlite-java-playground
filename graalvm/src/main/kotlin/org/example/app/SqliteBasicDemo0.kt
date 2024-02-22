package org.example.app

import java.util.logging.Logger
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import org.example.app.bindings.SqliteBindings
import org.example.app.sqlite3.Sqlite3CApi
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.plus
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback

class SqliteBasicDemo0(
    private val sqliteBindings: SqliteBindings,
    private val log: Logger = Logger.getLogger(SqliteBasicDemo1::class.simpleName)
) {
    private val api = Sqlite3CApi(sqliteBindings)
    private val memory = sqliteBindings.memoryBindings

    fun run() {
        printVersion()
        pringWasmEnumJson()
        // testDbSqliteVersion()

        repeat(3) { iteration ->
            val t = measureTime {
                testDbTable()
                //testLargeSqliteDatabase()
            }
            log.info { "Iteration $iteration time: $t" }
        }
    }

    private fun printVersion() {
        val (version, resultDuration) = measureTimedValue {
            api.version
        }
        log.info { "wasm: sqlite3_libversion_number = $version. duration: $resultDuration" }
    }

    private fun pringWasmEnumJson(): String? {
        return sqliteBindings.sqlite3WasmEnumJson.also {
            log.info { "wasm: $it" }
        }
    }

    private fun testDbSqliteVersion() {
        val dbPointer: WasmPtr<Sqlite3Db> = api.sqlite3Open("/home/work/test.db")
        // val dbPointer: Value = api.sqlite3Open(":memory:")

        try {
            val result = api.sqlite3Exec(dbPointer, "SELECT SQLITE_VERSION()");
            log.info { "result: $result" }
        } finally {
            api.sqlite3Close(dbPointer)
        }
    }

    private fun testDbTable() {
        val dbPointer: WasmPtr<Sqlite3Db> = api.sqlite3Open("/home/work/test7.db")
        //val dbPointer: Value = api.sqlite3Open(":memory:")

        val cb: Sqlite3ExecCallback = object: Sqlite3ExecCallback {
            override fun invoke(
                sqliteDb: WasmPtr<Sqlite3Db>,
                columns: Int,
                pResults: WasmPtr<WasmPtr<Byte>>,
                pColumnNames: WasmPtr<WasmPtr<Byte>>
            ): Int {
                log.info { "cb() db: $sqliteDb columns: $columns names: $pColumnNames results: $pResults" }
                val columnNames = (0 until columns).map { columnNo ->
                    val ptr = memory.readAddr(pColumnNames + (columnNo * WasmPtr.WASM_SIZEOF_PTR.toInt()))
                    memory.readNullTerminatedString(ptr)
                }

                val results =  (0 until columns).map { columnNo ->
                    val ptr = memory.readAddr(pResults + (columnNo * WasmPtr.WASM_SIZEOF_PTR.toInt()))
                    memory.readNullTerminatedString(ptr)
                }

                log.info { ": names: $columnNames results: $results" }

                return 0
            }
        }

        try {
            val requests = listOf(
                //"CREATE TABLE User(id INTEGER PRIMARY KEY, name TEXT);",
                //"""INSERT INTO User(`id`, `name`) VALUES (1, 'user 1'), (2, 'user 2'), (3, 'user 3');""",
                """SELECT * FROM User;"""
            )
            for ((requestNo, sql) in requests.withIndex()) {
                val result = api.sqlite3Exec(dbPointer, sql, cb)
                log.info { "REQ $requestNo (`${sql}`): result: $result" }
                if (result is Sqlite3Result.Error) {
                    break
                }
            }
        } finally {
            api.sqlite3Close(dbPointer)
        }
    }

    private fun testLargeSqliteDatabase() {
        val dbPointer: WasmPtr<Sqlite3Db> = api.sqlite3Open("/home/work/comments_dataset.db")

        try {
            val requests = listOf(
                """SELECT distinct `author.name` from comments order by `author.name` limit 10 offset 5000;"""
            )
            for ((requestNo, sql) in requests.withIndex()) {
                val result = api.sqlite3Exec(dbPointer, sql)
                log.info { "REQ $requestNo (`${sql}`): result: $result" }
                if (result is Sqlite3Result.Error) {
                    break
                }
            }
        } finally {
            api.sqlite3Close(dbPointer)
        }
    }
}