
plugins {
    id("ru.pixnews.gradle.wasm.sqlite3")

}


val defaultSqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()

sqlite3Build {
    builds {
        create("main") {
            sqliteVersion = defaultSqliteVersion
        }
        create("main2") {
            sqliteVersion = defaultSqliteVersion
        }
    }
}