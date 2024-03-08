package ru.pixnews.gradle.wasm.sqlite3

internal object BuildDirPath {
    internal val COMPILE_WORK_DIR = "emscripten/work"
    internal val STRIPPED_RESULT_DIR = "emscripten/out"

    internal fun compileUnstrippedResultDir(buildName: String): String = "emscripten/unstripped-${buildName}"
}