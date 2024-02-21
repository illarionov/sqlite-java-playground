package org.example.app.sqlite3

import org.example.app.bindings.SqliteBindings
import org.example.app.bindings.SqliteMemoryBindings
import org.graalvm.polyglot.Value
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.WasmPtr.Companion.SQLITE3_NULL
import ru.pixnews.wasm.host.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.host.WasmPtr.Companion.sqlite3Null
import ru.pixnews.wasm.host.isSqlite3Null

class Sqlite3CApi(
    private val bindings: SqliteBindings
) {
    private val memory: SqliteMemoryBindings = bindings.memoryBindings

    val version: Sqlite3Version
        get() = Sqlite3Version(
            bindings.sqlite3Version,
            bindings.sqlite3VersionNumber,
            bindings.sqlite3SourceId,
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

            val result: Value = bindings.sqlite3_open.execute(pFileName.addr, ppDb.addr)

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

    fun sqlite3Close(
        sqliteDb: WasmPtr<Sqlite3Db>
    ) {
        // TODO: __dbCleanupMap.cleanup(pDb)
        bindings.sqlite3_close_v2.execute(sqliteDb.addr)
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): String? {
        val p = bindings.sqlite3_errmsg.execute(sqliteDb.addr)
        return memory.readNullTerminatedString(p)
    }

    fun sqlite3ErrCode(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Int {
        return bindings.sqlite3_errcode.execute(sqliteDb.addr).asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<Sqlite3Db>
    ): Int {
        return bindings.sqlite3_extended_errcode.execute(sqliteDb.addr).asInt()
    }

    fun sqlite3Exec(
        sqliteDb: WasmPtr<Sqlite3Db>,
        sql: String,
    ) : Sqlite3Result<Unit> {
        var pSql: WasmPtr<Byte> = sqlite3Null()
        var pzErrMsg: WasmPtr<WasmPtr<Byte>> = sqlite3Null()
        try {
            pSql = memory.allocNullTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)

            val errNo = bindings.sqlite3_exec.execute(
                /* sqlite3* */ sqliteDb.addr,
                /* const char *sql */ pSql.addr,
                /* int (*callback)(void*,int,char**,char**) */ SQLITE3_NULL.addr,
                /* void * */ SQLITE3_NULL.addr,
                /* char **errmsg */ pzErrMsg.addr,
            ).asInt()

            if (errNo == Errno.SUCCESS.code) {
                return Sqlite3Result.Success(Unit)
            } else {
                val errMsgAddr: WasmPtr<Byte> = memory.readAddr(pzErrMsg)
                val errMsg = memory.readNullTerminatedString(errMsgAddr)
                memory.freeSilent(errMsgAddr)
                return Sqlite3Result.Error(
                    errNo,
                    errNo,
                    errMsg,
                )
            }
        } finally {
            memory.freeSilent(pSql)
            memory.freeSilent(pzErrMsg)
        }
    }

    private fun Value.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<Sqlite3Db>,
    ) {
        val errNo = this.asInt()
        if (errNo != Errno.SUCCESS.code) {
            val extendedErrCode: Int
            val errMsg: String
            if (!sqliteDb.isSqlite3Null()) {
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