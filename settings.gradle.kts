
pluginManagement {
    includeBuild("build-logic")
    //includeBuild("vendor/plainc")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "sqlite-java-playground"
include("graalvm", "sqlite3-wasm", "sqlite3-wasm-src", "chicory", "host", "android")
