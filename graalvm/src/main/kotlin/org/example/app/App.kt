package org.example.app

import java.util.logging.LogManager
import kotlin.time.measureTimedValue
import org.example.app.bindings.SqliteBindings
import org.example.app.host.Host
import org.example.app.host.emscrypten.EmscriptenEnvBindings
import org.example.app.host.preview1.WasiSnapshotPreview1BuiltinsModule
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.wasm.WasmContext
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm
import ru.pixnews.wasm.host.filesystem.FileSystem

private object App

fun main() {
    App::class.java.getResource("logging.properties")!!.openStream().use {
        LogManager.getLogManager().readConfiguration(it)
    }

    testSqlite()
}

private fun testSqlite() {
    val (sqlite3Bindings, evalDuration) = measureTimedValue {
        val wasmContext: Context = Context.newBuilder("wasm")
            .allowAllAccess(true)
            //.option("wasm.Builtins", "wasi_snapshot_preview1")
            .build()
        wasmContext.initialize("wasm")

        val host = Host(
            fileSystem = FileSystem()
        )

        wasmContext.enter()
        try {
            val instanceContext = WasmContext.get(null)

            EmscriptenEnvBindings.setupEnvBindings(instanceContext, host)

            val wasiInstance = WasiSnapshotPreview1BuiltinsModule().createInstance(
                instanceContext.language(),
                instanceContext,
                "wasi_snapshot_preview1"
            )
            instanceContext.register(wasiInstance)

        } finally {
            wasmContext.leave()
        }

        val sqliteSource: Source = run {
            val sqliteUrl = Sqlite3Wasm.Emscripten.sqlite3_346_o0_debug
            Source.newBuilder("wasm", sqliteUrl).build()
        }
        wasmContext.eval(sqliteSource)

        val wasmMainBindings = wasmContext.getBindings("wasm")

        println("keys: ${wasmMainBindings.memberKeys}")

        SqliteBindings(
            wasmContext.getBindings("wasm").getMember("env"),
            wasmContext.getBindings("wasm").getMember("main")
        )
    }
    println("wasm: binding = ${sqlite3Bindings.sqlite3_initialize}. duration: $evalDuration")

    // SqliteBasicDemo1(sqlite3Bindings).run()
    val demo0 = SqliteBasicDemo0(sqlite3Bindings)
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