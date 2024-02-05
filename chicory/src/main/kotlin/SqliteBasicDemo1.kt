import com.dylibso.chicory.wasm.types.Value
import java.util.logging.Logger
import kotlin.time.measureTimedValue
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.sqlite3.Sqlite3CApi

class SqliteBasicDemo1(
    private val sqliteBindings: SqliteBindings,
    private val log: Logger = Logger.getLogger(SqliteBasicDemo1::class.simpleName)
) {
    private val api = Sqlite3CApi(sqliteBindings)
    fun run() {
        printVersion()
        pringWasmEnumJson()
        openDb()
    }

    private fun printVersion() {
        val (version, resultDuration) = measureTimedValue {
            api.version
        }
        log.info { "wasm: sqlite3_libversion_number = $version. duration: $resultDuration" }
    }

    private fun pringWasmEnumJson(): String? {
        return sqliteBindings.wasmEnumJson.also {
            log.info { "wasm: $it" }
        }
    }

    private fun openDb() {
        val dbPointer: Value = sqliteBindings.sqlite3Open("test.db")

        sqliteBindings.sqlite3Close(dbPointer)
    }

}