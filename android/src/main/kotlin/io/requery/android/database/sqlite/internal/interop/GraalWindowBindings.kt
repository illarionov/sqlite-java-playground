package io.requery.android.database.sqlite.internal.interop

class GraalWindowBindings : SqlOpenHelperWindowBindings<Graallite3WindowPtr> {
    override fun nullPtr(): Graallite3WindowPtr {
        TODO("Not yet implemented")
    }

    override fun nativeCreate(name: String, cursorWindowSize: Int): Graallite3WindowPtr {
        TODO("Not yet implemented")
    }

    override fun nativeGetName(windowPtr: Graallite3WindowPtr): String? {
        TODO("Not yet implemented")
    }

    override fun nativePutNull(windowPtr: Graallite3WindowPtr, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutDouble(windowPtr: Graallite3WindowPtr, value: Double, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutLong(windowPtr: Graallite3WindowPtr, value: Long, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutString(windowPtr: Graallite3WindowPtr, value: String, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativePutBlob(windowPtr: Graallite3WindowPtr, value: ByteArray, row: Int, column: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeGetDouble(windowPtr: Graallite3WindowPtr, row: Int, column: Int): Double {
        TODO("Not yet implemented")
    }

    override fun nativeGetLong(windowPtr: Graallite3WindowPtr, row: Int, column: Int): Long {
        TODO("Not yet implemented")
    }

    override fun nativeGetString(windowPtr: Graallite3WindowPtr, row: Int, column: Int): String {
        TODO("Not yet implemented")
    }

    override fun nativeGetBlob(windowPtr: Graallite3WindowPtr, row: Int, column: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun nativeGetType(windowPtr: Graallite3WindowPtr, row: Int, column: Int): Int {
        TODO("Not yet implemented")
    }

    override fun nativeFreeLastRow(windowPtr: Graallite3WindowPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeAllocRow(windowPtr: Graallite3WindowPtr): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeSetNumColumns(windowPtr: Graallite3WindowPtr, columnNum: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun nativeGetNumRows(windowPtr: Graallite3WindowPtr): Int {
        TODO("Not yet implemented")
    }

    override fun nativeClear(windowPtr: Graallite3WindowPtr) {
        TODO("Not yet implemented")
    }

    override fun nativeDispose(windowPtr: Graallite3WindowPtr) {
        TODO("Not yet implemented")
    }
}