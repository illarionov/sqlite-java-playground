package ru.pixnews.sqlite3.wasm.util

interface Sqlite3BitMaskExt<T: Sqlite3BitMaskExt<T>> {
    val newInstance: (Int) -> T
    val mask: Int
}

fun Sqlite3BitMaskExt<*>.contains(flags: Sqlite3BitMaskExt<*>): Boolean = this.mask and flags.mask == flags.mask

infix fun <T: Sqlite3BitMaskExt<T>>Sqlite3BitMaskExt<T>.and(flags: Sqlite3BitMaskExt<*>): T = newInstance(mask and flags.mask)
infix fun <T: Sqlite3BitMaskExt<T>>Sqlite3BitMaskExt<T>.or(flags: Sqlite3BitMaskExt<*>): T = newInstance(mask or flags.mask)
infix fun <T: Sqlite3BitMaskExt<T>>Sqlite3BitMaskExt<T>.xor(flags: Sqlite3BitMaskExt<*>): T = newInstance(mask xor flags.mask)

infix fun <T: Sqlite3BitMaskExt<T>>Sqlite3BitMaskExt<T>.clear(flags: Sqlite3BitMaskExt<*>): T = newInstance(mask and flags.mask.inv())

