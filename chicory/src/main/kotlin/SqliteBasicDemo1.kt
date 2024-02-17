import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import kotlin.time.measureTime
import kotlin.time.measureTimedValue
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.sqlite3.Sqlite3CApi
import ru.pixnews.sqlite3.wasm.Sqlite3Result

class SqliteBasicDemo1(
    private val sqliteBindings: SqliteBindings,
    private val log: Logger = Logger.getLogger(SqliteBasicDemo1::class.qualifiedName)
) {
    private val api = Sqlite3CApi(
        bindings = sqliteBindings,
    )

    fun run() {
        printVersion()
        pringWasmEnumJson()
        // testDbSqliteVersion()

        val t = measureTime {
            testDbTable()
            // testLargeSqliteDatabase()
        }
        log.info { "time: $t" }
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
        val dbPointer: Value = api.sqlite3Open("/home/work/test.db")
        // val dbPointer: Value = api.sqlite3Open(":memory:")

        try {
            val result = api.sqlite3Exec(dbPointer, "SELECT SQLITE_VERSION()");
            log.info { "result: $result" }
        } finally {
            api.sqlite3Close(dbPointer)
        }
    }

    private fun testDbTable() {
        val dbPointer: Value = api.sqlite3Open("/home/work/test7.db")
        //val dbPointer: Value = api.sqlite3Open(":memory:")

        try {
            val requests = listOf(
                "CREATE TABLE User(id INTEGER PRIMARY KEY, name TEXT);",
                """INSERT INTO User(`id`, `name`) VALUES (1, 'user 1'), (2, 'user 2'), (3, 'user 3');""",
                """SELECT * FROM User;"""
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

    private fun testLargeSqliteDatabase() {
        val dbPointer: Value = api.sqlite3Open("/home/work/comments_dataset.db")
        //val dbPointer: Value = api.sqlite3Open(":memory:")

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