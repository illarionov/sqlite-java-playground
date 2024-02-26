package io.requery.android.database.sqlite

/**
 * Describes a SQLite extension entry point.
 * @param path path to the loadable extension shared library e.g. "/data/data/(package)/lib/libSqliteICU.so"
 * @param entryPoint extension entry point (optional) e.g. "sqlite3_icu_init"
 */
public data class SQLiteCustomExtension(
    val path: String,
    val entryPoint: String
)