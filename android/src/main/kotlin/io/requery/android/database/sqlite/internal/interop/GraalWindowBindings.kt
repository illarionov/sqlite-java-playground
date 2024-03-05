package io.requery.android.database.sqlite.internal.interop

import android.database.sqlite.SQLiteException

class GraalWindowBindings : SqlOpenHelperWindowBindings<GraalSqlite3WindowPtr> {
    override fun nativeCreate(name: String, cursorWindowSize: Int): GraalSqlite3WindowPtr {
        return GraalSqlite3WindowPtr(NativeCursorWindow(name, cursorWindowSize))
    }

    override fun nativeGetName(windowPtr: GraalSqlite3WindowPtr): String {
        return windowPtr.ptr.name
    }

    override fun nativePutNull(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putNull(row, column) == 0
    }

    override fun nativePutDouble(windowPtr: GraalSqlite3WindowPtr, value: Double, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putDouble(row, column, value) == 0
    }

    override fun nativePutLong(windowPtr: GraalSqlite3WindowPtr, value: Long, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putLong(row, column, value) == 0
    }

    override fun nativePutString(windowPtr: GraalSqlite3WindowPtr, value: String, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putString(row, column, value) == 0
    }

    override fun nativePutBlob(windowPtr: GraalSqlite3WindowPtr, value: ByteArray, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putBlob(row, column, value) == 0
    }

    override fun nativeGetDouble(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Double {
        val slot: NativeCursorWindow.FieldSlot =  windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> throw SQLiteException("Unable to convert BLOB to double")
                is NativeCursorWindow.Field.FloatField -> field.value
                is NativeCursorWindow.Field.IntegerField -> field.value.toDouble()
                NativeCursorWindow.Field.Null -> 0.0
                is NativeCursorWindow.Field.StringField -> field.value.toDoubleOrNull() ?: 0.0
            }
        }
    }

    override fun nativeGetLong(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Long {
        val slot: NativeCursorWindow.FieldSlot =  windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> throw SQLiteException("Unable to convert BLOB to double")
                is NativeCursorWindow.Field.FloatField -> field.value.toLong()
                is NativeCursorWindow.Field.IntegerField -> field.value
                NativeCursorWindow.Field.Null -> 0L
                is NativeCursorWindow.Field.StringField -> field.value.toLongOrNull() ?: 0L
            }
        }
    }

    override fun nativeGetString(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): String? {
        val slot: NativeCursorWindow.FieldSlot =  windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> throw SQLiteException("Unable to convert BLOB to double")
                is NativeCursorWindow.Field.FloatField -> field.value.toString()
                is NativeCursorWindow.Field.IntegerField -> field.value.toString()
                NativeCursorWindow.Field.Null -> null
                is NativeCursorWindow.Field.StringField -> field.value
            }
        }
    }

    override fun nativeGetBlob(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): ByteArray? {
        val slot: NativeCursorWindow.FieldSlot =  windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> field.value
                is NativeCursorWindow.Field.FloatField -> throw SQLiteException("FLOAT data in nativeGetBlob")
                is NativeCursorWindow.Field.IntegerField -> throw SQLiteException("INTEGER data in nativeGetBlob")
                NativeCursorWindow.Field.Null -> null
                is NativeCursorWindow.Field.StringField -> field.value.encodeToByteArray()
            }
        }
    }

    override fun nativeGetType(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): NativeCursorWindow.CursorFieldType {
        val slot: NativeCursorWindow.FieldSlot =  windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.type
    }

    override fun nativeFreeLastRow(windowPtr: GraalSqlite3WindowPtr) {
        windowPtr.ptr.freeLastRow()
    }

    override fun nativeAllocRow(windowPtr: GraalSqlite3WindowPtr): Boolean {
        return windowPtr.ptr.allocRow() == 0
    }

    override fun nativeSetNumColumns(windowPtr: GraalSqlite3WindowPtr, columnNum: Int): Boolean {
        return windowPtr.ptr.setNumColumns(columnNum) == 0
    }

    override fun nativeGetNumRows(windowPtr: GraalSqlite3WindowPtr): Int {
        return windowPtr.ptr.numRows
    }

    override fun nativeClear(windowPtr: GraalSqlite3WindowPtr) {
        windowPtr.ptr.clear()
    }

    override fun nativeDispose(windowPtr: GraalSqlite3WindowPtr) {
        windowPtr.ptr.clear()
    }
}