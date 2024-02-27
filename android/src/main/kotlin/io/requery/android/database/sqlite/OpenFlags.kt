package io.requery.android.database.sqlite

@JvmInline
public value class OpenFlags(
    override val mask: Int
) : SQLiteBitMask<OpenFlags> {
    override val newInstance: (Int) -> OpenFlags get() = ::OpenFlags

    companion object {
        val EMPTY: OpenFlags = OpenFlags(0)

        /** Open flag to open in the database in read only mode  */
        val OPEN_READONLY: OpenFlags = OpenFlags(0x00000001)

        /** Open flag to open in the database in read/write mode  */
        val OPEN_READWRITE: OpenFlags = OpenFlags(0x00000002)

        /** Open flag to create the database if it does not exist  */
        val OPEN_CREATE: OpenFlags = OpenFlags(0x00000004)

        /** Open flag to support URI filenames  */
        val OPEN_URI: OpenFlags = OpenFlags(0x00000040)

        /** Open flag opens the database in multi-thread threading mode  */
        val OPEN_NOMUTEX: OpenFlags = OpenFlags(0x00008000)

        /** Open flag opens the database in serialized threading mode  */
        val OPEN_FULLMUTEX: OpenFlags = OpenFlags(0x00010000)

        /** Open flag opens the database in shared cache mode  */
        val OPEN_SHAREDCACHE: OpenFlags = OpenFlags(0x00020000)

        /** Open flag opens the database in private cache mode  */
        val OPEN_PRIVATECACHE: OpenFlags = OpenFlags(0x00040000)

        /** Open flag equivalent to [.OPEN_READWRITE] | [.OPEN_CREATE]  */
        val CREATE_IF_NECESSARY: OpenFlags = OPEN_READWRITE or OPEN_CREATE

        /** Open flag to enable write-ahead logging  */ // custom flag remove for sqlite3_open_v2
        val ENABLE_WRITE_AHEAD_LOGGING: OpenFlags = OpenFlags(0x20000000)
    }
}