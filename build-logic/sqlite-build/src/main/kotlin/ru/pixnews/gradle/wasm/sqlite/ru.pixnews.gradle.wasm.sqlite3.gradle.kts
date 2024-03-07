import ru.pixnews.gradle.wasm.emscripten.EmscriptenBuildTask
import ru.pixnews.gradle.wasm.emscripten.WasmStripTask
import ru.pixnews.gradle.wasm.sqlite.BuildDirPath.STRIPPED_RESULT_DIR
import ru.pixnews.gradle.wasm.sqlite.SqliteAdditionalArgumentProvider
import ru.pixnews.gradle.wasm.sqlite.setupSqliteSource
import ru.pixnews.gradle.wasm.sqlite.sqliteRepository

// Convention Plugin for building Sqlite WASM using Emscripten

plugins {
    base
}

repositories {
    sqliteRepository()
}

val sqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()
val sqliteWasmFilesSrdDir = layout.projectDirectory.dir("src/main/cpp/sqlite")
val sqlite3c = setupSqliteSource(sqliteVersion)

val unstrippedFileNamePrefix = "sqlite3-${sqliteVersion}-unstripped"
val unstrippedJs = "$unstrippedFileNamePrefix.js"
val unstrippedWasm = "$unstrippedFileNamePrefix.wasm"
val strippedWasm = "sqlite3-${sqliteVersion}-stripped.wasm"

val compileSqliteTask = tasks.register<EmscriptenBuildTask>("compileSqlite") {
    val sqlite3cFile = sqlite3c.elements.map { it.first().asFile }

    group = "Build"
    description = "Compiles SQLite to Wasm"
    source.from(sqliteWasmFilesSrdDir.file("wasm/api/sqlite3-wasm.c"))
    outputFileName = "${unstrippedFileNamePrefix}.js"
    emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
    includes.setFrom(
        sqlite3cFile.map { it.parentFile },
        sqliteWasmFilesSrdDir.dir("wasm/api"),
    )
    additionalArgumentProviders.add(SqliteAdditionalArgumentProvider(sqlite3cFile))
}

val stripSqliteTask = tasks.register<WasmStripTask>("stripSqlite") {
    group = "Build"
    description = "Strips compiled SQLite binary"
    source.set(compileSqliteTask.flatMap { it.outputDirectory.file(unstrippedWasm) })
    destination.set(layout.buildDirectory.dir(STRIPPED_RESULT_DIR).map { it.file(strippedWasm) })
}

val wasmSqliteElements = configurations.consumable("wasmSqliteElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
    }
    outgoing {
        artifacts {
            artifact(stripSqliteTask.flatMap(WasmStripTask::destination)) {
                builtBy(stripSqliteTask)
            }
        }
    }
}

tasks.named("assemble").configure {
    dependsOn(stripSqliteTask)
}
