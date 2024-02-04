package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * Information about a pre-opened capability.
 */
public sealed class Prestat(
    public open val tag: Preopentype
) {
    public data class Dir(
        val prestatDir: ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.PrestatDir
    ) : Prestat(Preopentype.DIR)
}