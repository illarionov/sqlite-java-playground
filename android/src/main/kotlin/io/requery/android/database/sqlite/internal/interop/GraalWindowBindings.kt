package io.requery.android.database.sqlite.internal.interop

class GraalWindowBindings : SqlOpenHelperWindowBindings<GraalSqlite3WindowPtr> {
    override fun nullPtr(): GraalSqlite3WindowPtr {
        TODO("Not yet implemented")
    }

    override fun nativeCreate(name: String, cursorWindowSize: Int): GraalSqlite3WindowPtr {
        TODO("Not yet implemented")
    }

    override fun nativeGetName(windowPtr: GraalSqlite3WindowPtr): String? {
        TODO("Not yet implemented")
    }

    override fun nativePutNull(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutDouble(windowPtr: GraalSqlite3WindowPtr, value: Double, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutLong(windowPtr: GraalSqlite3WindowPtr, value: Long, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutString(windowPtr: GraalSqlite3WindowPtr, value: String, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutBlob(windowPtr: GraalSqlite3WindowPtr, value: ByteArray, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeGetDouble(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Double {
        TODO("Not yet implemented")
    }

    override fun nativeGetLong(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Long {
        TODO("Not yet implemented")
    }

    override fun nativeGetString(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): String {
        TODO("Not yet implemented")
    }

    override fun nativeGetBlob(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun nativeGetType(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Int {
        TODO("Not yet implemented")
    }

    override fun nativeFreeLastRow(windowPtr: GraalSqlite3WindowPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeAllocRow(windowPtr: GraalSqlite3WindowPtr): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeSetNumColumns(windowPtr: GraalSqlite3WindowPtr, columnNum: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeGetNumRows(windowPtr: GraalSqlite3WindowPtr): Int {
        TODO("Not yet implemented")
    }

    override fun nativeClear(windowPtr: GraalSqlite3WindowPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeDispose(windowPtr: GraalSqlite3WindowPtr) {
        TODO("Not yet implemented")
    }
}