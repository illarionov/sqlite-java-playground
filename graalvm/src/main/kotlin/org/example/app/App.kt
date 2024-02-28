package org.example.app

import java.util.logging.LogManager
import kotlin.time.measureTimedValue
import org.example.app.sqlite3.Sqlite3CApi

private object App

fun main() {
    App::class.java.getResource("logging.properties")!!.openStream().use {
        LogManager.getLogManager().readConfiguration(it)
    }
    testSqlite()
}

private fun testSqlite() {
    val (sqlite3Api, evalDuration) = measureTimedValue {
        Sqlite3CApi()
    }
    println("wasm: binding = ${sqlite3Api.sqliteBindings.sqlite3_initialize}. duration: $evalDuration")

    // SqliteBasicDemo1(sqlite3Bindings).run()
    val demo0 = SqliteBasicDemo0(sqlite3Api)
    demo0.run()
}
