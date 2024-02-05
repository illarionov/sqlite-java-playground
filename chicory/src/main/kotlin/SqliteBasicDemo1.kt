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
    }

    private fun printVersion() {
        val (version, resultDuration) = measureTimedValue {
            api.version
        }
        println("wasm: sqlite3_libversion_number = $version. duration: $resultDuration")
    }

    private fun pringWasmEnumJson(): String? {
        return sqliteBindings.wasmEnumJson.also {
            println("wasm: $it")
        }
    }
}