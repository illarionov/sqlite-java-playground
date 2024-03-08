import org.gradle.api.file.FileCollection
import ru.pixnews.gradle.wasm.emscripten.EmscriptenBuildTask
import ru.pixnews.gradle.wasm.emscripten.WasmStripTask
import ru.pixnews.gradle.wasm.ext.capitalizeAscii
import ru.pixnews.gradle.wasm.sqlite3.BuildDirPath.STRIPPED_RESULT_DIR
import ru.pixnews.gradle.wasm.sqlite3.BuildDirPath.compileUnstrippedResultDir
import ru.pixnews.gradle.wasm.sqlite3.Sqlite3WasmBuildSpec
import ru.pixnews.gradle.wasm.sqlite3.SqliteAdditionalArgumentProvider
import ru.pixnews.gradle.wasm.sqlite3.WasmSqlite3Extension
import ru.pixnews.gradle.wasm.sqlite3.createSqliteSourceConfiguration
import ru.pixnews.gradle.wasm.sqlite3.setupUnpackSqliteAttributes
import ru.pixnews.gradle.wasm.sqlite3.sqliteRepository

// Convention Plugin for building Sqlite WASM using Emscripten

plugins {
    base
}

repositories {
    sqliteRepository()
}

setupUnpackSqliteAttributes()

val wasmSqliteElements = configurations.consumable("wasmSqliteElements") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("wasm-library"))
    }
}

val sqliteExtension = extensions.create("sqlite3Build", WasmSqlite3Extension::class.java)

sqliteExtension.builds.configureEach {
    setupTasksForBuild(this)
}

fun setupTasksForBuild(buildSpec: Sqlite3WasmBuildSpec) {
    val buildName = buildSpec.name.capitalizeAscii()
    val sqliteWasmFilesSrdDir = layout.projectDirectory.dir("src/main/cpp/sqlite")
    val sqlite3c: FileCollection = if (buildSpec.sqlite3Source.isEmpty) {
        createSqliteSourceConfiguration(buildSpec.sqliteVersion.get())
    } else {
        buildSpec.sqlite3Source
    }
    val unstrippedWasmFileName = buildSpec.wasmUnstrippedFileName.get()
    val unstrippedJsFileName = unstrippedWasmFileName.substringBeforeLast(".wasm") + ".js"
    val strippedWasm = buildSpec.wasmFileName.get()

    val compileSqliteTask = tasks.register<EmscriptenBuildTask>("compileSqlite${buildName}") {
        val sqlite3cFile = sqlite3c.elements.map { it.first().asFile }

        group = "Build"
        description = "Compiles SQLite `${buildName}` to Wasm"
        source.from(sqliteWasmFilesSrdDir.file("wasm/api/sqlite3-wasm.c"))
        outputFileName = unstrippedJsFileName
        outputDirectory = layout.buildDirectory.dir(compileUnstrippedResultDir(buildName))
        emccVersion = versionCatalogs.named("libs").findVersion("emscripten").get().toString()
        includes.setFrom(
            sqlite3cFile.map { it.parentFile },
            sqliteWasmFilesSrdDir.dir("wasm/api"),
        )
        additionalArgumentProviders.add(SqliteAdditionalArgumentProvider(sqlite3cFile))
    }

    val stripSqliteTask = tasks.register<WasmStripTask>("stripSqlite${buildName}") {
        group = "Build"
        description = "Strips compiled SQLite `${buildName}` Wasn binary"
        source.set(compileSqliteTask.flatMap { it.outputDirectory.file(unstrippedWasmFileName) })
        destination.set(layout.buildDirectory.dir(STRIPPED_RESULT_DIR).map { it.file(strippedWasm) })
    }

    wasmSqliteElements.get().outgoing {
        artifacts {
            artifact(stripSqliteTask.flatMap(WasmStripTask::destination)) {
                builtBy(stripSqliteTask)
            }
        }
    }
    tasks.named("assemble").configure {
        dependsOn(stripSqliteTask)
    }
}



