package io.requery.android.database.sqlite

interface SQLiteBitMask<T: SQLiteBitMask<T>> {
    val newInstance: (Int) -> T
    val mask: Int
}

fun SQLiteBitMask<*>.contains(flags: SQLiteBitMask<*>): Boolean = this.mask and flags.mask == flags.mask

infix fun <T: SQLiteBitMask<T>>SQLiteBitMask<T>.or(flags: SQLiteBitMask<*>): T = newInstance(mask or flags.mask)
infix fun <T: SQLiteBitMask<T>>SQLiteBitMask<T>.xor(flags: SQLiteBitMask<*>): T = newInstance(mask xor flags.mask)

infix fun <T: SQLiteBitMask<T>>SQLiteBitMask<T>.clear(flags: SQLiteBitMask<*>): T = newInstance(mask and flags.mask.inv())

