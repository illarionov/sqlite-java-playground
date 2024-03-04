package org.example.app.sqlite3

import java.net.URL
import java.time.Clock
import org.example.app.bindings.SqliteBindings
import org.example.app.bindings.SqliteMemoryBindings
import org.example.app.ext.asWasmAddr
import org.example.app.ext.readNullTerminatedString
import org.example.app.ext.withWasmContext
import org.example.app.host.Host
import org.example.app.host.emscripten.EmscriptenEnvModuleBuilder
import org.example.app.host.preview1.WasiSnapshotPreview1MobuleBuilder
import org.example.app.sqlite3.callback.Sqlite3CallbackFunctionIndexes
import org.example.app.sqlite3.callback.Sqlite3CallbackStore
import org.example.app.sqlite3.callback.Sqlite3CallbackStore.Sqlite3ExecCallbackId
import org.example.app.sqlite3.callback.SqliteCallbacksModuleBuilder
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType
import ru.pixnews.sqlite3.wasm.Sqlite3DbStatusParameter
import ru.pixnews.sqlite3.wasm.Sqlite3DestructorType.SQLITE_TRANSIENT
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.sqlite3.wasm.Sqlite3ErrorInfo
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.sqlite3.wasm.Sqlite3TextEncoding
import ru.pixnews.sqlite3.wasm.Sqlite3TraceEventCode
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.host.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.isSqlite3Null
import ru.pixnews.wasm.host.memory.write
import ru.pixnews.wasm.host.plus
import ru.pixnews.wasm.host.sqlite3.Sqlite3ComparatorCallback
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback
import ru.pixnews.wasm.host.sqlite3.Sqlite3ProgressCallback
import ru.pixnews.wasm.host.sqlite3.Sqlite3Statement
import ru.pixnews.wasm.host.sqlite3.Sqlite3TraceCallback

fun Sqlite3CApi(
    graalvmEngine: Engine = Engine.create("wasm"),
    sqlite3Url: URL = Sqlite3Wasm.Emscripten.sqlite3_346_o2
): Sqlite3CApi {
    val callbackStore = Sqlite3CallbackStore()
    val host = Host(
        systemEnvProvider = System::getenv,
        commandArgsProvider = ::emptyList,
        fileSystem = FileSystem(),
        clock = Clock.systemDefaultZone(),
    )
    val graalContext: Context = Context.newBuilder("wasm")
        .engine(graalvmEngine)
        .allowAllAccess(true)
        .build()
    graalContext.initialize("wasm")

    val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(graalContext, host, callbackStore)
    graalContext.withWasmContext {
        EmscriptenEnvModuleBuilder(graalContext, host).setupModule()
        WasiSnapshotPreview1MobuleBuilder(graalContext, host).setupModule()
        sqliteCallbacksModuleBuilder.setupModule()
    }

    val sqliteSource: Source = Source.newBuilder("wasm", sqlite3Url).build()
    graalContext.eval(sqliteSource)

    val indirectFunctionIndexes = sqliteCallbacksModuleBuilder.setupIndirectFunctionTable()

    val bindings = SqliteBindings(graalContext)
    return Sqlite3CApi(bindings, callbackStore, indirectFunctionIndexes)
}

class Sqlite3CApi internal constructor(
    val sqliteBindings: SqliteBindings,
    val callbackStore: Sqlite3CallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
) {
    private val memory: SqliteMemoryBindings = sqliteBindings.memoryBindings

    val sqlite3Version: String
        get() {
            val resultPtr = sqliteBindings.sqlite3_libversion.execute()
            return checkNotNull(memory.memory.readNullTerminatedString(resultPtr))
        }

    val sqlite3SourceId: String
        get() {
            val resultPtr = sqliteBindings.sqlite3_sourceid.execute()
            return checkNotNull(memory.memory.readNullTerminatedString(resultPtr))
        }

    val sqlite3VersionNumber: Int
        get() = sqliteBindings.sqlite3_libversion_number.execute().asInt()

    val sqlite3WasmEnumJson: String?
        get() {
            val resultPtr = sqliteBindings.sqlite3_wasm_enum_json.execute()
            return memory.memory.readNullTerminatedString(resultPtr)
        }

    val sqlite3VersionFull: Sqlite3Version
        get() = Sqlite3Version(
            sqlite3Version,
            sqlite3VersionNumber,
            sqlite3SourceId,
        )

    data class Sqlite3Version(
        val version: String,
        val versionNumber: Int,
        val sourceId: String,
    )

    fun sqlite3Open(
        filename: String,
    ): WasmPtr<Sqlite3Db> {
        var ppDb: WasmPtr<WasmPtr<Sqlite3Db>> = sqlite3Null()
        var pFileName: WasmPtr<Byte> = sqlite3Null()
        var pDb: WasmPtr<Sqlite3Db> = sqlite3Null()
        try {
            ppDb = memory.allocOrThrow(WASM_SIZEOF_PTR)
            pFileName = memory.allocZeroTerminatedString(filename)

            val result: Value = sqliteBindings.sqlite3_open.execute(pFileName.addr, ppDb.addr)

            pDb = memory.readAddr(ppDb)
            result.throwOnSqliteError("sqlite3_open() failed", pDb)

            return pDb
        } catch (e: Throwable) {
            sqlite3Close(pDb)
            throw e
        } finally {
            memory.freeSilent(ppDb)
            memory.freeSilent(pFileName)
        }
    }

    fun sqlite3OpenV2(
        filename: String,
        flags: Sqlite3OpenFlags,
        vfsName: String?
    ): WasmPtr<Sqlite3Db> {
        var ppDb: WasmPtr<WasmPtr<Sqlite3Db>> = sqlite3Null()
        var pFileName: WasmPtr<Byte> = sqlite3Null()
        var pVfsName: WasmPtr<Byte> = sqlite3Null()
        var pDb: WasmPtr<Sqlite3Db> = sqlite3Null()
        try {
            ppDb = memory.allocOrThrow(WASM_SIZEOF_PTR)
            pFileName = memory.allocZeroTerminatedString(filename)
            if (vfsName != null) {
                pVfsName = memory.allocZeroTerminatedString(vfsName)
            }

            val result: Value = sqliteBindings.sqlite3_open_v2.execute(
                pFileName.addr,
                ppDb.addr,
                flags.mask,
                pVfsName.addr
            )

            pDb = memory.readAddr(ppDb)
            result.throwOnSqliteError("sqlite3_open_v2() failed", pDb)

            return pDb
        } catch (e: Throwable) {
            sqlite3Close(pDb)
            throw e
        } finally {
            memory.freeSilent(ppDb)
            memory.freeSilent(pFileName)
            memory.freeSilent(pVfsName)
        }
    }

    fun sqlite3Close(
        sqliteDb: WasmPtr<Sqlite3Db>
    ) {
        // TODO: __dbCleanupMap.cleanup(pDb)
        sqliteBindings.sqlite3_close_v2.execute(sqliteDb.addr)
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): String? {
        val p = sqliteBindings.sqlite3_errmsg.execute(sqliteDb.addr)
        return memory.readZeroTerminatedString(p)
    }

    fun sqlite3ErrCode(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Int {
        return sqliteBindings.sqlite3_errcode.execute(sqliteDb.addr).asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Int {
        return sqliteBindings.sqlite3_extended_errcode.execute(sqliteDb.addr).asInt()
    }

    fun sqlite3CreateCollation(
        database: WasmPtr<Sqlite3Db>,
        name: String,
        comparator: Sqlite3ComparatorCallback?,
    ) {
        val pCallbackId: Sqlite3CallbackStore.Sqlite3ComparatorId? = if (comparator != null) {
            callbackStore.sqlite3Comparators.put(comparator)
        } else {
            null
        }

        val pName: WasmPtr<Byte> = memory.allocZeroTerminatedString(name)

        val errNo = sqliteBindings.sqlite3_create_collation_v2.execute(
            database.addr,
            pName.addr,
            Sqlite3TextEncoding.SQLITE_UTF8.id,
            pCallbackId?.id,
            if (pCallbackId != null) callbackFunctionIndexes.execCallbackFunction.funcId else 0,
            if (pCallbackId != null) callbackFunctionIndexes.destroyComparatorFunction.funcId else 0,
        )
        memory.freeSilent(pName)
        if (errNo.asInt() != Errno.SUCCESS.code && pCallbackId != null) {
            callbackStore.sqlite3Comparators.remove(pCallbackId)
        }
        errNo.throwOnSqliteError("sqlite3CreateCollation() failed", database)
    }

    fun sqlite3Exec(
        database: WasmPtr<Sqlite3Db>,
        sql: String,
        callback: Sqlite3ExecCallback? = null,
    ): Sqlite3Result<Unit> {
        var pSql: WasmPtr<Byte> = sqlite3Null()
        var pzErrMsg: WasmPtr<WasmPtr<Byte>> = sqlite3Null()
        val pCallbackId: Sqlite3ExecCallbackId? = if (callback != null) {
            callbackStore.sqlite3ExecCallbacks.put(callback)
        } else {
            null
        }

        try {
            pSql = memory.allocZeroTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)

            val errNo = sqliteBindings.sqlite3_exec.execute(
                database.addr,
                pSql.addr,
                if (pCallbackId != null) callbackFunctionIndexes.execCallbackFunction.funcId else 0,
                pCallbackId?.id ?: 0,
                pzErrMsg.addr
            ).asInt()

            if (errNo == Errno.SUCCESS.code) {
                return Sqlite3Result.Success(Unit)
            } else {
                val errMsgAddr: WasmPtr<Byte> = memory.readAddr(pzErrMsg)
                val errMsg = memory.readZeroTerminatedString(errMsgAddr)
                memory.freeSilent(errMsgAddr)
                return Sqlite3Result.Error(errNo, errNo, errMsg)
            }
        } finally {
            pCallbackId?.let { callbackStore.sqlite3ExecCallbacks.remove(it) }
            memory.freeSilent(pSql)
            memory.freeSilent(pzErrMsg)
        }
    }

    fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<Sqlite3Db>,
        dbName: String?,
    ): Sqlite3DbReadonlyResult {
        val pDbName = if (dbName != null) {
            memory.allocZeroTerminatedString(dbName)
        } else {
            sqlite3Null()
        }

        try {
            val readonlyResultId = sqliteBindings.sqlite3_db_readonly.execute(sqliteDb.addr, pDbName.addr).asInt()
            return Sqlite3DbReadonlyResult.fromId(readonlyResultId)
        } finally {
            memory.freeSilent(pDbName)
        }
    }

    fun sqlite3BusyTimeout(
        sqliteDb: WasmPtr<Sqlite3Db>,
        ms: Int
    ) {
        val result = sqliteBindings.sqlite3_busy_timeout.execute(sqliteDb.addr, ms)
        result.throwOnSqliteError("sqlite3BusyTimeout() failed", sqliteDb)
    }

    fun sqlite3Trace(
        sqliteDb: WasmPtr<Sqlite3Db>,
        mask: Sqlite3TraceEventCode,
        traceCallback: Sqlite3TraceCallback?,
    ) {
        // TODO: remove callback on close
        if (traceCallback != null) {
            callbackStore.sqlite3TraceCallbacks[sqliteDb] = traceCallback
        }

        val errNo = sqliteBindings.sqlite3_trace_v2.execute(
            sqliteDb.addr,
            mask.mask,
            if (traceCallback != null) callbackFunctionIndexes.traceFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (traceCallback == null || errNo.asInt() != Errno.SUCCESS.code) {
            callbackStore.sqlite3TraceCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3_trace_v2() failed", sqliteDb)
    }

    fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<Sqlite3Db>,
        instructions: Int,
        progressCallback: Sqlite3ProgressCallback?,
    ) {
        val activeCallback: Sqlite3ProgressCallback? = if (instructions >= 1) {
            progressCallback
        } else {
            null
        }

        // TODO: remove callback on close
        if (activeCallback != null) {
            callbackStore.sqlite3ProgressCallbacks[sqliteDb] = activeCallback
        }

        val errNo = sqliteBindings.sqlite3_progress_handler.execute(
            sqliteDb.addr,
            instructions,
            if (activeCallback != null) callbackFunctionIndexes.progressFunction.funcId else 0,
            sqliteDb.addr
        )

        if (activeCallback == null) {
            callbackStore.sqlite3ProgressCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3ProgressHandler() failed", sqliteDb)
    }

    fun sqlite3DbStatus(
        sqliteDb: WasmPtr<Sqlite3Db>,
        op: Sqlite3DbStatusParameter,
        resetFlag: Boolean
    ): Sqlite3DbStatusResult {
        var pCur: WasmPtr<Int> = sqlite3Null()
        var pHiwtr: WasmPtr<Int> = sqlite3Null()

        try {
            pCur = memory.allocOrThrow(4U)
            pHiwtr = memory.allocOrThrow(4U)

            val errCode = sqliteBindings.sqlite3_db_status.execute(
                sqliteDb.addr,
                op.id,
                pCur.addr,
                pHiwtr.addr,
                if (resetFlag) 1 else 0
            )
            errCode.throwOnSqliteError(null, sqliteDb)
            return Sqlite3DbStatusResult(0, 0)
        } finally {
            memory.freeSilent(pCur)
            memory.freeSilent(pHiwtr)
        }
    }

    fun sqlite3ColumnCount(
        statement: WasmPtr<Sqlite3Statement>,
    ): Int {
        return sqliteBindings.sqlite3_column_count.execute(statement.addr).asInt()
    }

    fun sqlite3ColumnText(
        statement: WasmPtr<Sqlite3Statement>,
        columnIndex: Int,
    ): String? {
        val ptr = sqliteBindings.sqlite3_column_text.execute(
            statement.addr,
            columnIndex,
        ).asWasmAddr<Byte>()
        return memory.readZeroTerminatedString(ptr)
    }

    fun sqlite3ColumnInt64(
        statement: WasmPtr<Sqlite3Statement>,
        columnIndex: Int,
    ): Long {
        return sqliteBindings.sqlite3_column_int64.execute(
            statement.addr,
            columnIndex,
        ).asLong()
    }

    fun sqlite3ColumnDouble(
        statement: WasmPtr<Sqlite3Statement>,
        columnIndex: Int,
    ): Double {
        return sqliteBindings.sqlite3_column_double.execute(
            statement.addr,
            columnIndex,
        ).asDouble()
    }

    fun sqlite3ColumnBlob(
        statement: WasmPtr<Sqlite3Statement>,
        columnIndex: Int,
    ): ByteArray {
        val ptr = sqliteBindings.sqlite3_column_text.execute(
            statement.addr,
            columnIndex,
        ).asWasmAddr<Byte>()
        val bytes = sqliteBindings.sqlite3_column_bytes.execute(
            statement.addr,
            columnIndex
        ).asInt()
        return memory.memory.readBytes(ptr, bytes)
    }

    fun sqlite3Step(
        statement: WasmPtr<Sqlite3Statement>,
    ): Sqlite3Errno {
        val errCode = sqliteBindings.sqlite3_step.execute(statement.addr).asInt()
        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3Reset(
        statement: WasmPtr<Sqlite3Statement>,
    ): Sqlite3Errno {
        val errCode = sqliteBindings.sqlite3_reset.execute(statement.addr).asInt()
        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3ColumnType(
        statement: WasmPtr<Sqlite3Statement>,
        columnIndex: Int,
    ): Sqlite3ColumnType {
        val type = sqliteBindings.sqlite3_column_type.execute(statement.addr, columnIndex).asInt()
        return Sqlite3ColumnType(type)
    }

    fun readSqliteErrorInfo(
        sqliteDb: WasmPtr<Sqlite3Db>,
    ): Sqlite3ErrorInfo {
        if (sqliteDb.isSqlite3Null()) {
            return Sqlite3ErrorInfo(Errno.SUCCESS.code, Errno.SUCCESS.code, null)
        }

        val errCode = sqlite3ErrCode(sqliteDb)
        val extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
        val errMsg = if (errCode != 0) {
            sqlite3ErrMsg(sqliteDb) ?: "null"
        } else {
            null
        }
        return Sqlite3ErrorInfo(errCode, extendedErrCode, errMsg)
    }

    private fun Value.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<Sqlite3Db>,
    ) {
        val errNo = this.asInt()
        if (errNo != Errno.SUCCESS.code) {
            val errInfo = readSqliteErrorInfo(sqliteDb)
            throw Sqlite3Exception(errInfo, msgPrefix)
        }
    }

    fun sqlite3Changes(sqliteDb: WasmPtr<Sqlite3Db>): Int {
        return sqliteBindings.sqlite3_changes.execute(sqliteDb.addr).asInt()
    }

    fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Long {
        return sqliteBindings.sqlite3_last_insert_rowid.execute(sqliteDb.addr).asLong()
    }

    fun sqlite3ClearBindings(statement: WasmPtr<Sqlite3Statement>): Sqlite3Errno {
        val errCode = sqliteBindings.sqlite3_clear_bindings.execute(statement).asInt()
        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindBlobTransient(
        sqliteDb: WasmPtr<Sqlite3Statement>,
        index: Int,
        value: ByteArray,
    ): Sqlite3Errno {
        val pValue = memory.allocOrThrow<Byte>(value.size.toUInt())
        memory.memory.write(pValue, value, 0, value.size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_blob.execute(
                sqliteDb.addr,
                index,
                pValue.addr,
                value.size,
                SQLITE_TRANSIENT // TODO: change to destructor?
            ).asInt()
        } finally {
            memory.freeSilent(pValue)
        }

        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindStringTransient(
        sqliteDb: WasmPtr<Sqlite3Statement>,
        index: Int,
        value: String
    ): Sqlite3Errno {
        val encoded = value.encodeToByteArray()
        val size = encoded.size

        val pValue = memory.allocOrThrow<Byte>(size.toUInt())
        memory.memory.write(pValue, encoded, 0, size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_text.execute(
                sqliteDb.addr,
                index,
                pValue.addr,
                size,
                SQLITE_TRANSIENT // TODO: change to destructor?
            ).asInt()
        } finally {
            memory.freeSilent(pValue)
        }

        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindDouble(
        sqliteDb: WasmPtr<Sqlite3Statement>,
        index: Int,
        value: Double
    ): Any {
        val errCode = sqliteBindings.sqlite3_bind_double.execute(
            sqliteDb.addr,
            index,
            value
        ).asInt()
        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindLong(
        sqliteDb: WasmPtr<Sqlite3Statement>,
        index: Int,
        value: Long
    ): Any {
        val errCode = sqliteBindings.sqlite3_bind_int64.execute(
            sqliteDb.addr,
            index,
            value
        ).asInt()
        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindNull(
        sqliteDb: WasmPtr<Sqlite3Statement>,
        index: Int,
    ): Any {
        val errCode = sqliteBindings.sqlite3_bind_int64.execute(sqliteDb.addr, index,).asInt()
        return Sqlite3Errno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3ColumnName(
        statement: WasmPtr<Sqlite3Statement>,
        index: Int
    ): String? {
        val ptr = sqliteBindings.sqlite3_column_name.execute(statement.addr, index).asWasmAddr<Byte>()
        return memory.readZeroTerminatedString(ptr)
    }

    fun sqlite3StmtReadonly(statement: WasmPtr<Sqlite3Statement>): Boolean {
        return sqliteBindings.sqlite3_stmt_readonly.execute(statement.addr).asInt() != 0
    }

    fun sqlite3BindParameterCount(statement: WasmPtr<Sqlite3Statement>): Int {
        return sqliteBindings.sqlite3_bind_parameter_count.execute(statement.addr).asInt()
    }

    fun sqlite3PrepareV2(
        sqliteDb: WasmPtr<Sqlite3Db>,
        sql: String
    ): WasmPtr<Sqlite3Statement> {
        var sqlBytesPtr: WasmPtr<Byte> = sqlite3Null()
        var ppStatement: WasmPtr<WasmPtr<Sqlite3Statement>> = sqlite3Null()

        try {
            val sqlEncoded = sql.encodeToByteArray()
            val nullTerminatedSqlSize = sqlEncoded.size + 1

            sqlBytesPtr = memory.allocOrThrow(nullTerminatedSqlSize.toUInt())
            ppStatement = memory.allocOrThrow(WASM_SIZEOF_PTR)

            memory.memory.write(sqlBytesPtr, sqlEncoded)
            memory.memory.writeByte(sqlBytesPtr + sqlEncoded.size, 0)

            val result = sqliteBindings.sqlite3_prepare_v2.execute(
                sqliteDb.addr,
                sqlBytesPtr.addr,
                nullTerminatedSqlSize,
                ppStatement.addr,
                sqlite3Null<Unit>().addr
            )
            result.throwOnSqliteError("sqlite3_prepare_v2() failed", sqliteDb)
            return memory.readAddr(ppStatement)
        } finally {
            memory.freeSilent(sqlBytesPtr)
            memory.freeSilent(ppStatement)
        }
    }

    fun sqlite3Finalize(
        sqliteDatabase: WasmPtr<Sqlite3Db>,
        statement: WasmPtr<Sqlite3Statement>
    ) {
        val errCode = sqliteBindings.sqlite3_finalize.execute(statement.addr)
        errCode.throwOnSqliteError("sqlite3_finalize() failed", sqliteDatabase)
    }

    fun sqlite3ExpandedSql(statement: WasmPtr<Sqlite3Statement>): String? {
        val ptr = sqliteBindings.sqlite3_expanded_sql.execute(statement.addr)
        return memory.readZeroTerminatedString(ptr)
    }

    enum class Sqlite3DbReadonlyResult(val id: Int) {
        READ_ONLY(1),
        READ_WRITE(0),
        INVALID_NAME(-1);

        companion object {
            fun fromId(id: Int): Sqlite3DbReadonlyResult = entries.first { it.id == id }
        }
    }

    class Sqlite3DbStatusResult(
        val current: Int,
        val highestInstantaneousValue: Int,
    )
}