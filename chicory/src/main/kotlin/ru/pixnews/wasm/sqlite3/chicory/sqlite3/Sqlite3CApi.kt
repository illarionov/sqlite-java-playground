package ru.pixnews.wasm.sqlite3.chicory.sqlite3

import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.wasm.sqlite3.chicory.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite3.chicory.ext.SQLITE3_NULL
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.sqlite3.wasm.Sqlite3Version
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WASM_SIZEOF_PTR

class Sqlite3CApi(
    private val bindings: SqliteBindings,
    ) {
    private val memory = bindings.memoryBindings

    val version: Sqlite3Version
    get() = Sqlite3Version(
        bindings.sqlite3Version,
        bindings.sqlite3VersionNumber,
        bindings.sqlite3SourceId,
    )

    fun sqlite3Open(
        filename: String,
    ): Value {
        var ppDb: Value? = null
        var pFileName: Value? = null
        var pDb: Value? = null
        try {
            ppDb = memory.allocOrThrow(WASM_SIZEOF_PTR)
            pFileName = memory.allocNullTerminatedString(filename)

            val result = bindings.sqlite3_open.apply(pFileName, ppDb)

            pDb = memory.readAddr(ppDb.asWasmAddr())
            result.throwOnSqliteError("sqlite3_open() failed", pDb)

            return pDb
        } catch (e: Throwable) {
            pDb?.let { sqlite3Close(it) }
            throw e
        } finally {
            ppDb?.let { memory.freeSilent(it) }
            pFileName?.let { memory.freeSilent(it) }
        }
    }

    fun sqlite3Close(
        sqliteDb: Value
    ) {
        // TODO: __dbCleanupMap.cleanup(pDb)
        bindings.sqlite3_close_v2.apply(sqliteDb)
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: Value
    ): String? {
        val p = bindings.sqlite3_errmsg.apply(sqliteDb)[0]
        return memory.readNullTerminatedString(p)
    }

    fun sqlite3ErrCode(
        sqliteDb: Value
    ): Int {
        return bindings.sqlite3_errcode.apply(sqliteDb)[0].asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: Value
    ): Int {
        return bindings.sqlite3_extended_errcode.apply(sqliteDb)[0].asInt()
    }

    fun sqlite3Exec(
        sqliteDb: Value,
        sql: String,
    ) : Sqlite3Result<Unit> {
        var pSql: Value? = null
        var pzErrMsg: Value? = null
        try {
            pSql = memory.allocNullTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)

            val errNo = bindings.sqlite3_exec.apply(
                /* sqlite3* */ sqliteDb,
                /* const char *sql */ pSql,
                /* int (*callback)(void*,int,char**,char**) */ SQLITE3_NULL,
                /* void * */ SQLITE3_NULL,
                /* char **errmsg */ pzErrMsg,
            )[0].asInt()

            if (errNo == Errno.SUCCESS.code) {
                return Sqlite3Result.Success(Unit)
            } else {
                val errMsgAddr = memory.readAddr(pzErrMsg.asWasmAddr())
                val errMsg = memory.readNullTerminatedString(errMsgAddr)
                memory.freeSilent(errMsgAddr)
                return Sqlite3Result.Error(
                    errNo,
                    errNo,
                    errMsg,
                )
            }
        } finally {
            pSql?.let { memory.freeSilent(it) }
            if (pzErrMsg != null) {
                memory.freeSilent(pzErrMsg)
            }
        }
    }

    private fun Array<Value>.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: Value? = null,
    ) {
        check(this.size == 1) { "Not an errno" }
        val errNo = this[0].asInt()
        if (errNo != Errno.SUCCESS.code) {
            val extendedErrCode: Int
            val errMsg: String
            if (sqliteDb != null) {
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