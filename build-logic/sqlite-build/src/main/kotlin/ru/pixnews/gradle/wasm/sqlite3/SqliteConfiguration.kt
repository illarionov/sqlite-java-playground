package ru.pixnews.gradle.wasm.sqlite3

internal object SqliteConfiguration {
    val wasmConfig = listOf(
        """-DSQLITE_DEFAULT_UNIX_VFS="unix-none"""",
        "-DSQLITE_ENABLE_BYTECODE_VTAB",
        "-DSQLITE_ENABLE_DBPAGE_VTAB",
        "-DSQLITE_ENABLE_DBSTAT_VTAB",
        "-DSQLITE_ENABLE_EXPLAIN_COMMENTS",
        "-DSQLITE_ENABLE_FTS5",
        "-DSQLITE_ENABLE_OFFSET_SQL_FUNC",
        "-DSQLITE_ENABLE_RTREE",
        "-DSQLITE_ENABLE_STMTVTAB",
        "-DSQLITE_ENABLE_UNKNOWN_SQL_FUNCTION",
        "-DSQLITE_OMIT_DEPRECATED",
        "-DSQLITE_OMIT_LOAD_EXTENSION",
        "-DSQLITE_OMIT_SHARED_CACHE",
        "-DSQLITE_OMIT_UTF16",
        "-DSQLITE_OS_KV_OPTIONAL=1",
        "-DSQLITE_TEMP_STORE=2",
        "-DSQLITE_THREADSAFE=0",
        "-DSQLITE_USE_URI=1",
        "-DSQLITE_WASM_ENABLE_C_TESTS",
    )


    /**
     * Build configuration from https://github.com/requery/sqlite-android.git
     *
     *  NOTE the following flags,
     *  SQLITE_TEMP_STORE=3 causes all TEMP files to go into RAM. and thats the behavior we want
     *  SQLITE_ENABLE_FTS3  enables usage of FTS3 - NOT FTS1 or 2.
     *  SQLITE_DEFAULT_AUTOVACUUM=1 causes the databases to be subject to auto-vacuum
     */
    val requeryAndroidConfig = listOf(
        "-DHAVE_USLEEP=1",
        "-DNDEBUG=1",
        "-DSQLITE_DEFAULT_AUTOVACUUM=1",
        "-DSQLITE_DEFAULT_FILE_FORMAT=4",
        "-DSQLITE_DEFAULT_FILE_PERMISSIONS=0600",
        "-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576",
        "-DSQLITE_DEFAULT_MEMSTATUS=0",
        "-DSQLITE_ENABLE_BATCH_ATOMIC_WRITE",
        "-DSQLITE_ENABLE_FTS3",
        "-DSQLITE_ENABLE_FTS3_PARENTHESIS",
        "-DSQLITE_ENABLE_FTS4",
        "-DSQLITE_ENABLE_FTS4_PARENTHESIS",
        "-DSQLITE_ENABLE_FTS5",
        "-DSQLITE_ENABLE_FTS5_PARENTHESIS",
        "-DSQLITE_ENABLE_JSON1",
        "-DSQLITE_ENABLE_MEMORY_MANAGEMENT=1",
        "-DSQLITE_ENABLE_RTREE=1",
        "-DSQLITE_HAVE_ISNAN",
        "-DSQLITE_MAX_EXPR_DEPTH=0",
        "-DSQLITE_OMIT_COMPILEOPTION_DIAGS",
        "-DSQLITE_POWERSAFE_OVERWRITE=1",
        "-DSQLITE_TEMP_STORE=3",
        "-DSQLITE_THREADSAFE=2",
        "-DSQLITE_UNTESTABLE",
        "-DSQLITE_USE_ALLOCA",
        "-O3",
    )
}