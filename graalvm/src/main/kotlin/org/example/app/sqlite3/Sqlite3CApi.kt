package org.example.app.sqlite3

import org.example.app.bindings.SqliteBindings
import org.example.app.bindings.SqliteMemoryBindings
import org.example.app.ext.SQLITE3_NULL
import org.graalvm.polyglot.Value
import ru.pixnews.sqlite3.wasm.Sqlite3Exception
import ru.pixnews.sqlite3.wasm.Sqlite3Result
import ru.pixnews.wasm.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.host.wasi.preview1.type.WASM_SIZEOF_PTR
import ru.pixnews.wasm.host.wasi.preview1.type.WasmPtr

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
    ): WasmPtr {
        var ppDb: WasmPtr? = null
        var pFileName: WasmPtr? = null
        var pDb: WasmPtr? = null
        try {
            ppDb = memory.allocOrThrow(WASM_SIZEOF_PTR)
            pFileName = memory.allocNullTerminatedString(filename)

            val result: Value = bindings.sqlite3_open.execute(pFileName, ppDb)

            pDb = memory.readAddr(ppDb)
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
        sqliteDb: WasmPtr
    ) {
        // TODO: __dbCleanupMap.cleanup(pDb)
        bindings.sqlite3_close_v2.execute(sqliteDb)
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr
    ): String? {
        val p = bindings.sqlite3_errmsg.execute(sqliteDb)
        return memory.readNullTerminatedString(p)
    }

    fun sqlite3ErrCode(
        sqliteDb: WasmPtr
    ): Int {
        return bindings.sqlite3_errcode.execute(sqliteDb).asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr
    ): Int {
        return bindings.sqlite3_extended_errcode.execute(sqliteDb).asInt()
    }

    fun sqlite3Exec(
        sqliteDb: WasmPtr,
        sql: String,
    ) : Sqlite3Result<Unit> {
        var pSql: WasmPtr? = null
        var pzErrMsg: WasmPtr? = null
        try {
            pSql = memory.allocNullTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)

            val errNo = bindings.sqlite3_exec.execute(
                /* sqlite3* */ sqliteDb,
                /* const char *sql */ pSql,
                /* int (*callback)(void*,int,char**,char**) */ SQLITE3_NULL,
                /* void * */ SQLITE3_NULL,
                /* char **errmsg */ pzErrMsg,
            ).asInt()

            if (errNo == Errno.SUCCESS.code) {
                return Sqlite3Result.Success(Unit)
            } else {
                val errMsgAddr = memory.readAddr(pzErrMsg)
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

    private fun Value.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr? = null,
    ) {
        val errNo = this.asInt()
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