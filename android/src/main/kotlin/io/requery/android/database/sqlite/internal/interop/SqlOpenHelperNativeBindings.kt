package io.requery.android.database.sqlite.internal.interop

import io.requery.android.database.sqlite.SQLiteFunction
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags

interface SqlOpenHelperNativeBindings<
        CP : Sqlite3ConnectionPtr,
        SP : Sqlite3StatementPtr,
        WP : Sqlite3WindowPtr> {

    fun connectionNullPtr(): CP
    fun connectionStatementPtr(): SP
    fun connectionWindowPtr(): WP

    fun nativeOpen(
        path: String,
        openFlags: Sqlite3OpenFlags,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean
    ): CP

    fun nativeClose(
        connectionPtr: CP,
    )

    fun nativeRegisterFunction(
        connectionPtr: CP,
        function: SQLiteFunction
    )

    fun nativeRegisterLocalizedCollators(
        connectionPtr: CP,
        locale: String
    )

    fun nativePrepareStatement(
        connectionPtr: CP,
        sql: String
    ): SP

    fun nativeFinalizeStatement(
        connectionPtr: CP,
        statementPtr: SP,
    )

    fun nativeGetParameterCount(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeIsReadOnly(
        connectionPtr: CP,
        statementPtr: SP,
    ): Boolean

    fun nativeGetColumnCount(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeGetColumnName(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int
    ): String?

    fun nativeBindNull(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int
    )

    fun nativeBindLong(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: Long
    )

    fun nativeBindDouble(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: Double
    )

    fun nativeBindString(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: String
    )

    fun nativeBindBlob(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: ByteArray,
    )

    fun nativeResetStatementAndClearBindings(
        connectionPtr: CP,
        statementPtr: SP,
    )

    fun nativeExecute(
        connectionPtr: CP,
        statementPtr: SP,
    )

    fun nativeExecuteForLong(
        connectionPtr: CP,
        statementPtr: SP,
    ): Long

    fun nativeExecuteForString(
        connectionPtr: CP,
        statementPtr: SP,
    ): String?

    fun nativeExecuteForBlobFileDescriptor(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeExecuteForChangedRowCount(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeExecuteForLastInsertedRowId(
        connectionPtr: CP,
        statementPtr: SP,
    ): Long

    fun nativeExecuteForCursorWindow(
        connectionPtr: CP,
        statementPtr: SP,
        winPtr: WP,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean
    ): Long

    fun nativeGetDbLookaside(
        connectionPtr: CP,
    ): Int

    fun nativeCancel(
        connectionPtr: CP,
    )

    fun nativeResetCancel(
        connectionPtr: CP,
        cancelable: Boolean
    )
}
