pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "sqlite-java-playground"
include("graalvm", "sqlite3-wasm", "chicory")
