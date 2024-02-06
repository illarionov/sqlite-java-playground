package ru.pixnews.wasm.sqlite3.chicory.bindings

import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.WasiType


enum class Sqlite3Errno(
    public val code: Int,
) {
    /**
     * No error occurred. System call completed successfully.
     */
    SQLITE_OK(0),

    SQLITE_ERROR(1),   /* Generic error */
    SQLITE_INTERNAL(2),   /* Internal logic error in SQLite */
    SQLITE_PERM(3),   /* Access permission denied */
    SQLITE_ABORT(4),   /* Callback routine requested an abort */
    SQLITE_BUSY(5),   /* The database file is locked */
    SQLITE_LOCKED(6),   /* A table in the database is locked */
    SQLITE_NOMEM(7),   /* A malloc() failed */
    SQLITE_READONLY(8),   /* Attempt to write a readonly database */
    SQLITE_INTERRUPT(9),   /* Operation terminated by sqlite3_interrupt()*/
    SQLITE_IOERR(10),   /* Some kind of disk I/O error occurred */
    SQLITE_CORRUPT(11),   /* The database disk image is malformed */
    SQLITE_NOTFOUND(12),   /* Unknown opcode in sqlite3_file_control() */
    SQLITE_FULL(13),   /* Insertion failed because database is full */
    SQLITE_CANTOPEN(14),   /* Unable to open the database file */
    SQLITE_PROTOCOL(15),   /* Database lock protocol error */
    SQLITE_EMPTY(16),   /* Internal use only */
    SQLITE_SCHEMA(17),   /* The database schema changed */
    SQLITE_TOOBIG(18),   /* String or BLOB exceeds size limit */
    SQLITE_CONSTRAINT(19),   /* Abort due to constraint violation */
    SQLITE_MISMATCH(20),   /* Data type mismatch */
    SQLITE_MISUSE(21),   /* Library used incorrectly */
    SQLITE_NOLFS(22),   /* Uses OS features not supported on host */
    SQLITE_AUTH(23),   /* Authorization denied */
    SQLITE_FORMAT(24),   /* Not used */
    SQLITE_RANGE(25),   /* 2nd parameter to sqlite3_bind out of range */
    SQLITE_NOTADB(26),   /* File opened that is not a database file */
    SQLITE_NOTICE(27),   /* Notifications from sqlite3_log() */
    SQLITE_WARNING(28),   /* Warnings from sqlite3_log() */
    SQLITE_ROW(100),  /* sqlite3_step() has another row ready */
    SQLITE_DONE(101),  /* sqlite3_step() has finished executing */

    SQLITE_ERROR_MISSING_COLLSEQ((SQLITE_ERROR.code.or(1.shl(8)))),
    SQLITE_ERROR_RETRY((SQLITE_ERROR.code.or(2.shl(8)))),
    SQLITE_ERROR_SNAPSHOT((SQLITE_ERROR.code.or(3.shl(8)))),

    SQLITE_IOERR_READ((SQLITE_IOERR.code.or(1.shl(8)))),
    SQLITE_IOERR_SHORT_READ((SQLITE_IOERR.code.or(2.shl(8)))),
    SQLITE_IOERR_WRITE((SQLITE_IOERR.code.or(3.shl(8)))),
    SQLITE_IOERR_FSYNC((SQLITE_IOERR.code.or(4.shl(8)))),
    SQLITE_IOERR_DIR_FSYNC((SQLITE_IOERR.code.or(5.shl(8)))),
    SQLITE_IOERR_TRUNCATE((SQLITE_IOERR.code.or(6.shl(8)))),
    SQLITE_IOERR_FSTAT((SQLITE_IOERR.code.or(7.shl(8)))),
    SQLITE_IOERR_UNLOCK((SQLITE_IOERR.code.or(8.shl(8)))),
    SQLITE_IOERR_RDLOCK((SQLITE_IOERR.code.or(9.shl(8)))),
    SQLITE_IOERR_DELETE((SQLITE_IOERR.code.or(10.shl(8)))),
    SQLITE_IOERR_BLOCKED((SQLITE_IOERR.code.or(11.shl(8)))),
    SQLITE_IOERR_NOMEM((SQLITE_IOERR.code.or(12.shl(8)))),
    SQLITE_IOERR_ACCESS((SQLITE_IOERR.code.or(13.shl(8)))),
    SQLITE_IOERR_CHECKRESERVEDLOCK((SQLITE_IOERR.code.or(14.shl(8)))),
    SQLITE_IOERR_LOCK((SQLITE_IOERR.code.or(15.shl(8)))),
    SQLITE_IOERR_CLOSE((SQLITE_IOERR.code.or(16.shl(8)))),
    SQLITE_IOERR_DIR_CLOSE((SQLITE_IOERR.code.or(17.shl(8)))),
    SQLITE_IOERR_SHMOPEN((SQLITE_IOERR.code.or(18.shl(8)))),
    SQLITE_IOERR_SHMSIZE((SQLITE_IOERR.code.or(19.shl(8)))),
    SQLITE_IOERR_SHMLOCK((SQLITE_IOERR.code.or(20.shl(8)))),
    SQLITE_IOERR_SHMMAP((SQLITE_IOERR.code.or(21.shl(8)))),
    SQLITE_IOERR_SEEK((SQLITE_IOERR.code.or(22.shl(8)))),
    SQLITE_IOERR_DELETE_NOENT((SQLITE_IOERR.code.or(23.shl(8)))),
    SQLITE_IOERR_MMAP((SQLITE_IOERR.code.or(24.shl(8)))),
    SQLITE_IOERR_GETTEMPPATH((SQLITE_IOERR.code.or(25.shl(8)))),
    SQLITE_IOERR_CONVPATH((SQLITE_IOERR.code.or(26.shl(8)))),
    SQLITE_IOERR_VNODE((SQLITE_IOERR.code.or(27.shl(8)))),
    SQLITE_IOERR_AUTH((SQLITE_IOERR.code.or(28.shl(8)))),
    SQLITE_IOERR_BEGIN_ATOMIC((SQLITE_IOERR.code.or(29.shl(8)))),
    SQLITE_IOERR_COMMIT_ATOMIC((SQLITE_IOERR.code.or(30.shl(8)))),
    SQLITE_IOERR_ROLLBACK_ATOMIC((SQLITE_IOERR.code.or(31.shl(8)))),
    SQLITE_IOERR_DATA((SQLITE_IOERR.code.or(32.shl(8)))),
    SQLITE_IOERR_CORRUPTFS((SQLITE_IOERR.code.or(33.shl(8)))),
    SQLITE_IOERR_IN_PAGE((SQLITE_IOERR.code.or(34.shl(8)))),

    SQLITE_LOCKED_SHAREDCACHE((SQLITE_LOCKED.code.or(1.shl(8)))),
    SQLITE_LOCKED_VTAB((SQLITE_LOCKED.code.or(2.shl(8)))),

    SQLITE_BUSY_RECOVERY((SQLITE_BUSY.code.or(1.shl(8)))),
    SQLITE_BUSY_SNAPSHOT((SQLITE_BUSY.code.or(2.shl(8)))),
    SQLITE_BUSY_TIMEOUT((SQLITE_BUSY.code.or(3.shl(8)))),

    SQLITE_CANTOPEN_NOTEMPDIR((SQLITE_CANTOPEN.code.or(1.shl(8)))),
    SQLITE_CANTOPEN_ISDIR((SQLITE_CANTOPEN.code.or(2.shl(8)))),
    SQLITE_CANTOPEN_FULLPATH((SQLITE_CANTOPEN.code.or(3.shl(8)))),
    SQLITE_CANTOPEN_CONVPATH((SQLITE_CANTOPEN.code.or(4.shl(8)))),
    SQLITE_CANTOPEN_DIRTYWAL((SQLITE_CANTOPEN.code.or(5.shl(8)))), /* Not Used */
    SQLITE_CANTOPEN_SYMLINK((SQLITE_CANTOPEN.code.or(6.shl(8)))),

    SQLITE_CORRUPT_VTAB((SQLITE_CORRUPT.code.or(1.shl(8)))),
    SQLITE_CORRUPT_SEQUENCE((SQLITE_CORRUPT.code.or(2.shl(8)))),
    SQLITE_CORRUPT_INDEX((SQLITE_CORRUPT.code.or(3.shl(8)))),

    SQLITE_READONLY_RECOVERY((SQLITE_READONLY.code.or(1.shl(8)))),
    SQLITE_READONLY_CANTLOCK((SQLITE_READONLY.code.or(2.shl(8)))),
    SQLITE_READONLY_ROLLBACK((SQLITE_READONLY.code.or(3.shl(8)))),
    SQLITE_READONLY_DBMOVED((SQLITE_READONLY.code.or(4.shl(8)))),
    SQLITE_READONLY_CANTINIT((SQLITE_READONLY.code.or(5.shl(8)))),
    SQLITE_READONLY_DIRECTORY((SQLITE_READONLY.code.or(6.shl(8)))),

    SQLITE_ABORT_ROLLBACK((SQLITE_ABORT.code.or(2.shl(8)))),

    SQLITE_CONSTRAINT_CHECK((SQLITE_CONSTRAINT.code.or(1.shl(8)))),
    SQLITE_CONSTRAINT_COMMITHOOK((SQLITE_CONSTRAINT.code.or(2.shl(8)))),
    SQLITE_CONSTRAINT_FOREIGNKEY((SQLITE_CONSTRAINT.code.or(3.shl(8)))),
    SQLITE_CONSTRAINT_FUNCTION((SQLITE_CONSTRAINT.code.or(4.shl(8)))),
    SQLITE_CONSTRAINT_NOTNULL((SQLITE_CONSTRAINT.code.or(5.shl(8)))),
    SQLITE_CONSTRAINT_PRIMARYKEY((SQLITE_CONSTRAINT.code.or(6.shl(8)))),
    SQLITE_CONSTRAINT_TRIGGER((SQLITE_CONSTRAINT.code.or(7.shl(8)))),
    SQLITE_CONSTRAINT_UNIQUE((SQLITE_CONSTRAINT.code.or(8.shl(8)))),
    SQLITE_CONSTRAINT_VTAB((SQLITE_CONSTRAINT.code.or(9.shl(8)))),
    SQLITE_CONSTRAINT_ROWID((SQLITE_CONSTRAINT.code.or(10.shl(8)))),
    SQLITE_CONSTRAINT_PINNED((SQLITE_CONSTRAINT.code.or(11.shl(8)))),
    SQLITE_CONSTRAINT_DATATYPE((SQLITE_CONSTRAINT.code.or(12.shl(8)))),

    SQLITE_NOTICE_RECOVER_WAL((SQLITE_NOTICE.code.or(1.shl(8)))),
    SQLITE_NOTICE_RECOVER_ROLLBACK((SQLITE_NOTICE.code.or(2.shl(8)))),
    SQLITE_NOTICE_RBU((SQLITE_NOTICE.code.or(3.shl(8)))),

    SQLITE_WARNING_AUTOINDEX((SQLITE_WARNING.code.or(1.shl(8)))),

    SQLITE_AUTH_USER((SQLITE_AUTH.code.or(1.shl(8)))),

    SQLITE_OK_LOAD_PERMANENTLY((SQLITE_OK.code.or(1.shl(8)))),
    SQLITE_OK_SYMLINK((SQLITE_OK.code.or(2.shl(8)))), /* internal use only */

    ;

    public val value: Value get() = Value.i32(code.toLong())

    public companion object : WasiType {
        override val tag: ValueType = ValueType.I32

        val entriesMap: Map<Int, Sqlite3Errno> = entries.associateBy(Sqlite3Errno::code)

        fun fromErrNoCode(code: Int): Sqlite3Errno? = entriesMap[code]
    }
}