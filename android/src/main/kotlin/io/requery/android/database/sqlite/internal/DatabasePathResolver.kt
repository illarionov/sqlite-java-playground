package io.requery.android.database.sqlite.internal

import java.io.File

fun interface DatabasePathResolver {
    fun getDatabasePath(name: String): File
}