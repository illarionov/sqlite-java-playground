package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.sqlite3.wasm.Sqlite3Version
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr.Companion.SQLITE3_NULL
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.ext.asValue
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr

class Sqlite3CApi(
    private val bindings: SqliteBindings,
    ) {
    private val memory = bindings.memoryBindings
    private val callbackManager: Sqlite3CallbackManager = bindings.callbackManager

    val version: Sqlite3Version
    get() = Sqlite3Version(
        bindings.sqlite3Version,
        bindings.sqlite3VersionNumber,
        bindings.sqlite3SourceId,
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

            val result = bindings.sqlite3_open.apply(pFileName.asValue(), ppDb.asValue())

            pDb = memory.readAddr(ppDb)
            result.throwOnSqliteError("sqlite3_open() failed", pDb)

            return pDb
        } catch (e: Throwable) {
            sqlite3Close(pDb)
            throw e
        } finally {
            memory.freeSilent(pFileName)
            memory.freeSilent(ppDb)
        }
    }

    fun sqlite3Close(
        sqliteDb: WasmPtr<Sqlite3Db>
    ) {
        // TODO: __dbCleanupMap.cleanup(pDb)
        bindings.sqlite3_close_v2.apply(sqliteDb.asValue())
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): String? {
        val p = bindings.sqlite3_errmsg.apply(sqliteDb.asValue())[0]
        return memory.readNullTerminatedString(p.asWasmAddr())
    }

    fun sqlite3ErrCode(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Int {
        return bindings.sqlite3_errcode.apply(sqliteDb.asValue())[0].asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Int {
        return bindings.sqlite3_extended_errcode.apply(sqliteDb.asValue())[0].asInt()
    }

    fun sqlite3Exec(
        sqliteDb: WasmPtr<Sqlite3Db>,
        sql: String,
        callback: Sqlite3ExecCallback? = null,
    ) : Sqlite3Result<Unit> {
        var pSql: WasmPtr<Byte> = sqlite3Null()
        var pzErrMsg: WasmPtr<WasmPtr<Byte>> = sqlite3Null()
        var pCallback: WasmPtr<Sqlite3ExecCallback> = sqlite3Null()

        try {
            pSql = memory.allocNullTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)
            if (callback != null) {
                pCallback = callbackManager.registerExecCallback(callback)
            }

            val errNo = bindings.sqlite3Exec(
                /* sqlite3* */ sqliteDb,
                /* const char *sql */ pSql,
                /* int (*callback)(void*,int,char**,char**) */ pCallback,
                /* void * */ SQLITE3_NULL,
                /* char **errmsg */ pzErrMsg,
            )

            if (errNo == Errno.SUCCESS.code) {
                return Sqlite3Result.Success(Unit)
            } else {
                val errMsgAddr = memory.readAddr(pzErrMsg)
                val errMsg = memory.readNullTerminatedString(errMsgAddr)
                memory.freeSilent(errMsgAddr)
                return Sqlite3Result.Error(errNo, errNo, errMsg,)
            }
        } finally {
            memory.freeSilent(pSql)
            memory.freeSilent(pzErrMsg)
        }
    }

    private fun Array<Value>.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<Sqlite3Db> = sqlite3Null(),
    ) {
        check(this.size == 1) { "Not an errno" }
        val errNo = this[0].asInt()
        if (errNo != Errno.SUCCESS.code) {
            val extendedErrCode: Int
            val errMsg: String
            if (sqliteDb != SQLITE3_NULL) {
                extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
                errMsg = sqlite3ErrMsg(sqliteDb) ?: "null"
            } else {
                extendedErrCode = -1
                errMsg = ""
            }

            throw Sqlite3Exception(errNo, extendedErrCode, msgPrefix, errMsg)
        }
    }
}