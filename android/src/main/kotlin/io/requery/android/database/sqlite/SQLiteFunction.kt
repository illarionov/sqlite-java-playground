package io.requery.android.database.sqlite

import io.requery.android.database.sqlite.internal.SQLiteDatabase
import io.requery.android.database.sqlite.internal.SQLiteDatabaseFunction

/**
 * @author dhleong
 */
class SQLiteFunction @JvmOverloads constructor(
    val name: String,
    val numArgs: Int,
    val callback: SQLiteDatabaseFunction,
    flags: Int = 0
) {
    // accessed from native code
    val flags: Int

    // NOTE: from a single database connection, all calls to
    // functions are serialized by SQLITE-internal mutexes,
    // so we save on GC churn by reusing a single, shared instance
    private val args = MyArgs()
    private val result = MyResult()

    /**
     * Create custom function.
     *
     * @param name The name of the sqlite3 function.
     * @param numArgs The number of arguments for the function, or -1 to
     * support any number of arguments.
     * @param callback The callback to invoke when the function is executed.
     * @param flags Extra SQLITE flags to pass when creating the function
     * in native code.
     */
    /**
     * Create custom function.
     *
     * @param name The name of the sqlite3 function.
     * @param numArgs The number of arguments for the function, or -1 to
     * support any number of arguments.
     * @param callback The callback to invoke when the function is executed.
     * @param flags Extra SQLITE flags to pass when creating the function
     * in native code.
     */
    init {
        this.flags = flags
    }

    // Called from native.
    @Suppress("unused")
    private fun dispatchCallback(contextPtr: Long, argsPtr: Long, argsCount: Int) {
        result.contextPtr = contextPtr
        args.argsPtr = argsPtr
        args.argsCount = argsCount

        try {
            callback.callback(args, result)

            if (!result.isSet) {
                result.setNull()
            }
        } finally {
            result.contextPtr = 0
            result.isSet = false
            args.argsPtr = 0
            args.argsCount = 0
        }
    }

    private class MyArgs : SQLiteDatabaseFunction.Args {
        var argsPtr: Long = 0
        var argsCount: Int = 0

        override fun getBlob(arg: Int): ByteArray? {
            return nativeGetArgBlob(argsPtr, checkArg(arg))
        }

        override fun getString(arg: Int): String? {
            return nativeGetArgString(argsPtr, checkArg(arg))
        }

        override fun getDouble(arg: Int): Double {
            return nativeGetArgDouble(argsPtr, checkArg(arg))
        }

        override fun getInt(arg: Int): Int {
            return nativeGetArgInt(argsPtr, checkArg(arg))
        }

        override fun getLong(arg: Int): Long {
            return nativeGetArgLong(argsPtr, checkArg(arg))
        }

        private fun checkArg(arg: Int): Int {
            require(!(arg < 0 || arg >= argsCount)) { "Requested arg $arg but had $argsCount" }

            return arg
        }
    }

    private class MyResult : SQLiteDatabaseFunction.Result {
        var contextPtr: Long = 0
        var isSet: Boolean = false

        override fun set(value: ByteArray?) {
            checkSet()
            nativeSetResultBlob(contextPtr, value)
        }

        override fun set(value: Double) {
            checkSet()
            nativeSetResultDouble(contextPtr, value)
        }

        override fun set(value: Int) {
            checkSet()
            nativeSetResultInt(contextPtr, value)
        }

        override fun set(value: Long) {
            checkSet()
            nativeSetResultLong(contextPtr, value)
        }

        override fun set(value: String?) {
            checkSet()
            nativeSetResultString(contextPtr, value)
        }

        override fun setError(error: String?) {
            checkSet()
            nativeSetResultError(contextPtr, error)
        }

        override fun setNull() {
            checkSet()
            nativeSetResultNull(contextPtr)
        }

        private fun checkSet() {
            check(!isSet) { "Result is already set" }
            isSet = true
        }
    }

    companion object {
        external fun nativeGetArgBlob(argsPtr: Long, arg: Int): ByteArray?
        external fun nativeGetArgString(argsPtr: Long, arg: Int): String?
        external fun nativeGetArgDouble(argsPtr: Long, arg: Int): Double
        external fun nativeGetArgInt(argsPtr: Long, arg: Int): Int
        external fun nativeGetArgLong(argsPtr: Long, arg: Int): Long

        external fun nativeSetResultBlob(contextPtr: Long, result: ByteArray?)
        external fun nativeSetResultString(contextPtr: Long, result: String?)
        external fun nativeSetResultDouble(contextPtr: Long, result: Double)
        external fun nativeSetResultInt(contextPtr: Long, result: Int)
        external fun nativeSetResultLong(contextPtr: Long, result: Long)
        external fun nativeSetResultError(contextPtr: Long, error: String?)
        external fun nativeSetResultNull(contextPtr: Long)
    }
}