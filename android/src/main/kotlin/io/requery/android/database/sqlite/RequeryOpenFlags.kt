package io.requery.android.database.sqlite

import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags

@JvmInline
public value class RequeryOpenFlags(
    override val mask: Int
) : SQLiteBitMask<RequeryOpenFlags> {
    override val newInstance: (Int) -> RequeryOpenFlags get() = ::RequeryOpenFlags

    companion object {
        val EMPTY: RequeryOpenFlags = RequeryOpenFlags(0)

        /** Open flag to open in the database in read only mode  */
        val OPEN_READONLY: RequeryOpenFlags = RequeryOpenFlags(0x00000001)

        /** Open flag to open in the database in read/write mode  */
        val OPEN_READWRITE: RequeryOpenFlags = RequeryOpenFlags(0x00000002)

        /** Open flag to create the database if it does not exist  */
        val OPEN_CREATE: RequeryOpenFlags = RequeryOpenFlags(0x00000004)

        /** Open flag to support URI filenames  */
        val OPEN_URI: RequeryOpenFlags = RequeryOpenFlags(0x00000040)

        /** Open flag opens the database in multi-thread threading mode  */
        val OPEN_NOMUTEX: RequeryOpenFlags = RequeryOpenFlags(0x00008000)

        /** Open flag opens the database in serialized threading mode  */
        val OPEN_FULLMUTEX: RequeryOpenFlags = RequeryOpenFlags(0x00010000)

        /** Open flag opens the database in shared cache mode  */
        val OPEN_SHAREDCACHE: RequeryOpenFlags = RequeryOpenFlags(0x00020000)

        /** Open flag opens the database in private cache mode  */
        val OPEN_PRIVATECACHE: RequeryOpenFlags = RequeryOpenFlags(0x00040000)

        /** Open flag equivalent to [.OPEN_READWRITE] | [.OPEN_CREATE]  */
        val CREATE_IF_NECESSARY: RequeryOpenFlags = OPEN_READWRITE or OPEN_CREATE

        /** Open flag to enable write-ahead logging  */ // custom flag remove for sqlite3_open_v2
        val ENABLE_WRITE_AHEAD_LOGGING: RequeryOpenFlags = RequeryOpenFlags(0x20000000)
    }
}

internal fun RequeryOpenFlags.toSqliteOpenFlags(): Sqlite3OpenFlags = Sqlite3OpenFlags(
    (this clear RequeryOpenFlags.ENABLE_WRITE_AHEAD_LOGGING).mask
)