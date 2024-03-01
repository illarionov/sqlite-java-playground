package org.example.app.sqlite3

import java.net.URL
import java.time.Clock
import org.example.app.bindings.SqliteBindings
import org.example.app.bindings.SqliteMemoryBindings
import org.example.app.ext.asWasmAddr
import org.example.app.ext.functionTable
import org.example.app.ext.withWasmContext
import org.example.app.host.Host
import org.example.app.host.emscripten.EmscriptenEnvBindings
import org.example.app.host.preview1.WasiSnapshotPreview1Bindngs
import org.example.app.sqlite3.callback.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import org.example.app.sqlite3.callback.SQLITE3_EXEC_CB_FUNCTION_NAME
import org.example.app.sqlite3.callback.Sqlite3CallbackStore
import org.example.app.sqlite3.callback.Sqlite3CallbackStore.Sqlite3ExecCallbackId
import org.example.app.sqlite3.callback.setupSqliteCallbacksWasmModule
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.wasm.WasmFunctionInstance
import ru.pixnews.sqlite3.wasm.Sqlite3ColumnType
import ru.pixnews.sqlite3.wasm.Sqlite3DbStatusParameter
import ru.pixnews.sqlite3.wasm.Sqlite3Errno
import ru.pixnews.sqlite3.wasm.Sqlite3ErrorInfo
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3OpenFlags
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.sqlite3.wasm.Sqlite3TextEncoding
import ru.pixnews.sqlite3.wasm.Sqlite3Wasm
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.host.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.host.filesystem.FileSystem
import ru.pixnews.wasm.host.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.host.isSqlite3Null
import ru.pixnews.wasm.host.sqlite3.Sqlite3ComparatorCallbackRaw
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback
import ru.pixnews.wasm.host.sqlite3.Sqlite3Profile
import ru.pixnews.wasm.host.sqlite3.Sqlite3ProgressHandlerCallback
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

    graalContext.withWasmContext { instanceContext ->
        EmscriptenEnvBindings.setupEnvBindings(instanceContext, host)
        WasiSnapshotPreview1Bindngs.setupWasiSnapshotPreview1Bindngs(instanceContext, host)
        setupSqliteCallbacksWasmModule(instanceContext, callbackStore)
    }

    val sqliteSource: Source = Source.newBuilder("wasm", sqlite3Url).build()

    graalContext.eval(sqliteSource)

    // XXX: replace with globals?
    val sqliteExecFuncInstance = graalContext
        .getBindings("wasm")
        .getMember(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)
        .getMember(SQLITE3_EXEC_CB_FUNCTION_NAME)
        .`as`(WasmFunctionInstance::class.java)

    val sqlite3ExecCbFuncId = graalContext.withWasmContext { wasmContext ->
        val sqlite3ExecCbFuncId = wasmContext.functionTable.grow(1, sqliteExecFuncInstance)
        IndirectFunctionTableIndex(sqlite3ExecCbFuncId)
    }

    val bindings = SqliteBindings(graalContext, sqlite3ExecCbFuncId)
    return Sqlite3CApi(bindings, callbackStore)
}

class Sqlite3CApi internal constructor(
    val sqliteBindings: SqliteBindings,
    val callbackStore: Sqlite3CallbackStore,
) {
    private val memory: SqliteMemoryBindings = sqliteBindings.memoryBindings

    val version: Sqlite3Version
        get() = Sqlite3Version(
            sqliteBindings.sqlite3Version,
            sqliteBindings.sqlite3VersionNumber,
            sqliteBindings.sqlite3SourceId,
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
            pFileName = memory.allocNullTerminatedString(filename)

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
            pFileName = memory.allocNullTerminatedString(filename)
            if (vfsName != null) {
                pVfsName = memory.allocNullTerminatedString(vfsName)
            }

            val result: Value = sqliteBindings.sqlite3_open_v2.execute(
                pFileName.addr,
                ppDb.addr,
                flags.mask,
                pVfsName
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

    fun sqlite3CreateCollation(
        db: WasmPtr<Sqlite3Db>,
        name: String,
        encoding: Sqlite3TextEncoding,
        comparator: Sqlite3ComparatorCallbackRaw,
    ) {
        // TODO
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
        return memory.readNullTerminatedString(p)
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

    fun sqlite3Exec(
        sqliteDb: WasmPtr<Sqlite3Db>,
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
            pSql = memory.allocNullTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)

            val errNo = sqliteBindings.sqlite3Exec(
                sqliteDb = sqliteDb,
                pSql = pSql,
                callbackId = pCallbackId,
                pzErrMsg = pzErrMsg
            )

            if (errNo == Errno.SUCCESS.code) {
                return Sqlite3Result.Success(Unit)
            } else {
                val errMsgAddr: WasmPtr<Byte> = memory.readAddr(pzErrMsg)
                val errMsg = memory.readNullTerminatedString(errMsgAddr)
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
        // TODO
        val id = 0
        return Sqlite3DbReadonlyResult.fromId(id)
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
        callback: Sqlite3TraceCallback,
    ) {
        TODO()
    }

    fun sqlite3Profile(
        sqliteDb: WasmPtr<Sqlite3Db>,
        callback: Sqlite3Profile,
    ) {
        TODO()
    }

    fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<Sqlite3Db>,
        instructions: Int,
        callback: Sqlite3ProgressHandlerCallback?,
    ) {
        TODO()
    }

    fun sqlite3DbStatus(
        sqliteDb: WasmPtr<Sqlite3Db>,
        op: Sqlite3DbStatusParameter,
        resetFlag: Boolean
    ): Sqlite3DbStatusResult {
        // TODO

        return Sqlite3DbStatusResult(0, 0)
    }

    fun sqlite3ColumnCount(
        statement: WasmPtr<Sqlite3Statement>,
    ): Int {
        return sqliteBindings.sqlite3_column_count.execute(statement.addr).asInt()
    }

    fun sqlite3ColumnText(
        statement: WasmPtr<Sqlite3Statement>,
        columnIndex: Int,
    ): String {
        val ptr = sqliteBindings.sqlite3_column_text.execute(
            statement.addr,
            columnIndex,
        ).asWasmAddr<Byte>()
        return checkNotNull(memory.readNullTerminatedString(ptr))
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