package ru.pixnews.sqlite3.wasm

import ru.pixnews.sqlite3.wasm.util.Sqlite3BitMaskExt

@JvmInline
value class Sqlite3OpenFlags(
    override val mask: Int
) : Sqlite3BitMaskExt<Sqlite3OpenFlags> {
    override val newInstance: (Int) -> Sqlite3OpenFlags get() = ::Sqlite3OpenFlags

    companion object {
        val EMPTY: Sqlite3OpenFlags = Sqlite3OpenFlags(0)

        /** Open flag to open in the database in read only mode  */
        val SQLITE_OPEN_READONLY: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000001)

        /** Open flag to open in the database in read/write mode  */
        val SQLITE_OPEN_READWRITE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000002)

        /** Open flag to create the database if it does not exist  */
        val SQLITE_OPEN_CREATE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000004)

        /** VFS only */
        val SQLITE_OPEN_DELETEONCLOSE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000008)

        /** VFS only */
        val SQLITE_OPEN_EXCLUSIVE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000010)

        /** VFS only */
        val SQLITE_OPEN_AUTOPROXY: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000020)

        /** Open flag to support URI filenames  */
        val SQLITE_OPEN_URI: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000040)

        /** VFS only */
        val SQLITE_OPEN_MEMORY: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000080)

        /** VFS only */
        val SQLITE_OPEN_MAIN_DB: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000100)

        /** VFS only */
        val SQLITE_OPEN_TEMP_DB: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000200)

        /** VFS only */
        val SQLITE_OPEN_TRANSIENT_DB: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000400)

        /** VFS only */
        val SQLITE_OPEN_MAIN_JOURNAL: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00000800)

        /** VFS only */
        val SQLITE_OPEN_TEMP_JOURNAL: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00001000)

        /** VFS only */
        val SQLITE_OPEN_SUBJOURNAL: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00002000)

        /** VFS only */
        val SQLITE_OPEN_SUPER_JOURNAL: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00004000)

        /** Open flag opens the database in multi-thread threading mode  */
        val SQLITE_OPEN_NOMUTEX: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00008000)

        /** Open flag opens the database in serialized threading mode  */
        val SQLITE_OPEN_FULLMUTEX: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00010000)

        /** Open flag opens the database in shared cache mode  */
        val SQLITE_OPEN_SHAREDCACHE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00020000)

        /** Open flag opens the database in private cache mode  */
        val SQLITE_OPEN_PRIVATECACHE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00040000)

        /** VFS only */
        val SQLITE_OPEN_WAL: Sqlite3OpenFlags = Sqlite3OpenFlags(0x00080000)

        /** VFS only */
        val SQLITE_OPEN_NOFOLLOW: Sqlite3OpenFlags = Sqlite3OpenFlags(0x01000000)

        /** VFS only */
        val SQLITE_OPEN_EXRESCODE: Sqlite3OpenFlags = Sqlite3OpenFlags(0x02000000)
    }
}