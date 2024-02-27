package io.requery.android.database.sqlite.internal.interop

interface Sqlite3Ptr {
    fun isNull(): Boolean
}

fun Sqlite3Ptr.isNotNull() = !isNull()
interface Sqlite3ConnectionPtr : Sqlite3Ptr
interface Sqlite3StatementPtr : Sqlite3Ptr
interface Sqlite3WindowPtr : Sqlite3Ptr