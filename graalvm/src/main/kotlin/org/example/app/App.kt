package org.example.app

import java.time.Clock
import java.util.logging.LogManager
import kotlin.time.measureTimedValue
import org.example.app.bindings.SqliteBindings
import org.example.app.ext.functionTable
import org.example.app.ext.withWasmContext
import org.example.app.host.Host
import org.example.app.host.emscripten.EmscriptenEnvBindings.setupEnvBindings
import org.example.app.host.preview1.WasiSnapshotPreview1Bindngs.setupWasiSnapshotPreview1Bindngs
import org.example.app.sqlite3.callback.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import org.example.app.sqlite3.callback.SQLITE3_EXEC_CB_FUNCTION_NAME
import org.example.app.sqlite3.callback.Sqlite3CallbackStore
import org.example.app.sqlite3.callback.setupSqliteCallbacksWasmModule
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.wasm.WasmFunctionInstance
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableIndex

private object App

fun main() {
    App::class.java.getResource("logging.properties")!!.openStream().use {
        LogManager.getLogManager().readConfiguration(it)
    }

    testSqlite()
}

private fun testSqlite() {
    val callbackStore = Sqlite3CallbackStore()
    val host = Host(
        systemEnvProvider = System::getenv,
        commandArgsProvider = ::emptyList,
        fileSystem = FileSystem(),
        clock = Clock.systemDefaultZone(),
    )

    val (sqlite3Bindings, evalDuration) = measureTimedValue {
        val graalContext: Context = Context.newBuilder("wasm")
            .allowAllAccess(true)
            //.option("wasm.Builtins", "wasi_snapshot_preview1")
            .build()
        graalContext.initialize("wasm")

        graalContext.withWasmContext { instanceContext ->
            setupEnvBindings(instanceContext, host)
            setupWasiSnapshotPreview1Bindngs(instanceContext, host)
            setupSqliteCallbacksWasmModule(instanceContext, callbackStore)
        }

        val sqliteSource: Source = run {
            val sqliteUrl = Sqlite3Wasm.Emscripten.sqlite3_346_o2
            Source.newBuilder("wasm", sqliteUrl).build()
        }

        graalContext.eval(sqliteSource)

        // XXX: replace with globals?
        val sqliteExecFuncInstance = graalContext
            .getBindings("wasm")
            .getMember(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)
            .getMember(SQLITE3_EXEC_CB_FUNCTION_NAME)
            .`as`(WasmFunctionInstance::class.java)

        val sqlite3ExecCbFuncId = graalContext.withWasmContext { wasmContext ->
            val sqlite3ExecCbFuncId = wasmContext.functionTable.grow(1, sqliteExecFuncInstance)
            IndirectFunctionTableIndex(sqlite3ExecCbFuncId)
        }

        SqliteBindings(graalContext, sqlite3ExecCbFuncId)
    }
    println("wasm: binding = ${sqlite3Bindings.sqlite3_initialize}. duration: $evalDuration")

    // SqliteBasicDemo1(sqlite3Bindings).run()
    val demo0 = SqliteBasicDemo0(sqlite3Bindings, callbackStore)
    demo0.run()
}

private fun testFactorial() {
    val (factorial, evalDuration) = measureTimedValue {
        val wasmContext = Context.newBuilder("wasm")
            //.allowAllAccess(true)
            //.option("wasm.Builtins", "wasi_snapshot_preview1")
            .build()
        val factorialSource = run {
            val factorialWasmUrl = requireNotNull(App::class.java.getResource("ru/pixnews/sqlite3/wasm/factorial.wasm"))
            Source.newBuilder("wasm", factorialWasmUrl).build()
        }

        wasmContext.eval(factorialSource)
        wasmContext.getBindings("wasm")
            .getMember("main")
            .getMember("fac")
    }
    val (result, resultDuration) = measureTimedValue {
        factorial.execute(20L)
    }
    println("wasm: factorial(20) = $result. duration: $evalDuration / $resultDuration")
}