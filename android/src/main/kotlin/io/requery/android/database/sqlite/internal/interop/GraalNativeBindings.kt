package io.requery.android.database.sqlite.internal.interop

import io.requery.android.database.sqlite.SQLiteFunction
import org.example.app.sqlite3.Sqlite3CApi
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.isSqlite3Null
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3Statement

@JvmInline
value class GraalSqlite3ConnectionPtr(
    val ptr: WasmPtr<Sqlite3Db>
) : Sqlite3ConnectionPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

@JvmInline
value class GraalSqlite3StatementPtr(
    val ptr: WasmPtr<Sqlite3Statement>
) : Sqlite3StatementPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}
@JvmInline
value class Graallite3WindowPtr(
    val ptr: WasmPtr<Void>
) : Sqlite3WindowPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

class GraalNativeBindings(
    private val sqlite3Api: Sqlite3CApi,
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr, Graallite3WindowPtr> {
    override fun <T : Sqlite3Ptr> nullPtr(): T {
        TODO("Not yet implemented")
    }

    override fun nativeOpen(
        path: String,
        openFlags: Int,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean
    ): GraalSqlite3ConnectionPtr {

        TODO("Not yet implemented")
    }

    override fun nativeRegisterLocalizedCollators(connectionPtr: GraalSqlite3ConnectionPtr, locale: String) {
        TODO("Not yet implemented")
    }

    override fun nativeRegisterFunction(connectionPtr: GraalSqlite3ConnectionPtr, function: SQLiteFunction) {
        TODO("Not yet implemented")
    }

    override fun nativeClose(connectionPtr: GraalSqlite3ConnectionPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeHasCodec(): Boolean {

        TODO("Not yet implemented")
    }

    override fun nativeLoadExtension(connectionPtr: GraalSqlite3ConnectionPtr, file: String, proc: String) {
        TODO("Not yet implemented")
    }

    override fun nativeResetCancel(connectionPtr: GraalSqlite3ConnectionPtr, cancelable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun nativeCancel(connectionPtr: GraalSqlite3ConnectionPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeGetDbLookaside(connectionPtr: GraalSqlite3ConnectionPtr): Int {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForCursorWindow(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        winPtr: Graallite3WindowPtr,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean
    ): Long {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForLastInsertedRowId(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Long {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForChangedRowCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForBlobFileDescriptor(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): String? {
        TODO("Not yet implemented")
    }

    override fun nativeExecuteForLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Long {
        TODO("Not yet implemented")
    }

    override fun nativeExecute(connectionPtr: GraalSqlite3ConnectionPtr, statementPtr: GraalSqlite3StatementPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeResetStatementAndClearBindings(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindBlob(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: ByteArray
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: String
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindDouble(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Double
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Long
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeBindNull(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun nativeGetColumnName(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int
    ): String? {
        TODO("Not yet implemented")
    }

    override fun nativeGetColumnCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeIsReadOnly(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeGetParameterCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ): Int {
        TODO("Not yet implemented")
    }

    override fun nativeFinalizeStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr
    ) {
        TODO("Not yet implemented")
    }

    override fun nativePrepareStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        sql: String
    ): GraalSqlite3StatementPtr {
        TODO("Not yet implemented")
    }

}