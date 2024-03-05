package io.requery.android.database.sqlite.internal.interop

import co.touchlab.kermit.Logger
import java.nio.ByteBuffer

class NativeCursorWindow(
    val name: String,
    val size: Int,
    val isReadOnly: Boolean = false,
    logger: Logger = Logger
) {
    private var _freeSpace: Int = size
    private val logger = logger.withTag("NativeCursorWindow")
    private val data: Header = Header(0, RowSlotChunk(), 0, 0)

    val freeSpace: Int
        get() = _freeSpace

    val numRows: Int
        get() = data.numRows

    fun clear(): Int {
        if (isReadOnly) return -1
        data.freeOffset = 0
        data.firstChunk = RowSlotChunk()
        data.numColumns = 0
        data.numRows = 0
        return 0
    }

    fun setNumColumns(numColumns: Int): Int {
        if (isReadOnly) return -1

        data.numColumns.let {
            if ((it > 0 || data.numRows > 0) && numColumns != it) {
                logger.i { "Trying to go from $it columns to $numColumns" }
                return -1
            }
        }

        data.numColumns = numColumns
        return 0
    }

    fun allocRow(): Int {
        check(!isReadOnly)
        val slot = allocRowSlot()
        slot.fields = Array(data.numColumns) { FieldSlot() }
        // TODO fail on full window
        return 0
    }

    fun freeLastRow() {
        check(!isReadOnly)
        if (data.numRows > 0) {
            data.numRows -= 1
        }
    }

    private fun allocRowSlot(): RowSlot {
        check(!isReadOnly)

        var chunkPos = data.numRows
        var rowSlotChunk: RowSlotChunk = data.firstChunk
        while (chunkPos > ROW_SLOT_CHUNK_NUM_ROWS) {
            rowSlotChunk = rowSlotChunk.nextChunk!!
            chunkPos -= ROW_SLOT_CHUNK_NUM_ROWS
        }
        if (chunkPos == ROW_SLOT_CHUNK_NUM_ROWS) {
            RowSlotChunk().let {
                rowSlotChunk.nextChunk = it
                rowSlotChunk = it
            }
            chunkPos = 0
        }
        data.numRows += 1
        return rowSlotChunk.slots[chunkPos]
    }

    private fun getRowSlot(row: Int): RowSlot? {
        var chunkPos = row
        var rowSlotChunk: RowSlotChunk? = data.firstChunk
        while (chunkPos >= ROW_SLOT_CHUNK_NUM_ROWS) {
            rowSlotChunk = rowSlotChunk?.nextChunk
            chunkPos -= ROW_SLOT_CHUNK_NUM_ROWS
        }
        return rowSlotChunk?.let { it.slots[chunkPos] }
    }

    fun getFieldSlot(row: Int, column: Int): FieldSlot? {
        if (row >= data.numRows || column >= data.numColumns) {
            logger.e {
                "Failed to read row $row, column $column from a CursorWindow which " +
                        "has ${data.numRows} rows, ${data.numColumns} columns"
            }
            return null
        }
        val slot = getRowSlot(row) ?: run {
            logger.e { "Failed to find rowSlot for row $row" }
            return null
        }
        return slot.fields[column]
    }

    fun putBlob(row: Int, column: Int, value: ByteArray): Int {
        check(!isReadOnly)
        return putField(row, column, Field.BlobField(value))
    }

    fun putString(row: Int, column: Int, value: String): Int = putField(row, column, Field.StringField(value))

    fun putLong(row: Int, column: Int, value: Long): Int = putField(row, column, Field.IntegerField(value))

    fun putDouble(row: Int, column: Int, value: Double): Int = putField(row, column, Field.FloatField(value))
    fun putNull(row: Int, column: Int): Int = putField(row, column, Field.Null)

    private fun putField(row: Int, column: Int, value: Field): Int {
        check(!isReadOnly)
        val slot: FieldSlot = getFieldSlot(row, column) ?: return -1 // BAD_VALUE
        slot.field = value
        return 0
    }

    private class Header(
        /**
         * Offset of the lowest unused byte in the window
         */
        var freeOffset: Long,

        // firstChunkOffset
        var firstChunk: RowSlotChunk,

        var numRows: Int,
        var numColumns: Int
    )

    sealed class Field(
        val type: CursorFieldType
    ) {
        data object Null : Field(CursorFieldType.NULL)
        class IntegerField(val value: Long) : Field(CursorFieldType.INTEGER)
        class FloatField(val value: Double) : Field(CursorFieldType.FLOAT)
        class StringField(val value: String) : Field(CursorFieldType.STRING)
        class BlobField(val value: ByteArray) : Field(CursorFieldType.BLOB)
    }

    class FieldSlot(
        var field: Field = Field.Null
    ) {
        val type: CursorFieldType
            get() = this.field.type
    }

    private class RowSlot(numColumns: Int) {
        var fields: Array<FieldSlot> = Array(numColumns) { FieldSlot() }
    }

    private class RowSlotChunk {
        val slots: MutableList<RowSlot> = MutableList(ROW_SLOT_CHUNK_NUM_ROWS) { RowSlot(0) }
        var nextChunk: RowSlotChunk? = null
    }

    enum class CursorFieldType(val id: Int) {
        NULL(0),
        INTEGER(1),
        FLOAT(2),
        STRING(3),
        BLOB(4)
    }

    companion object {
        private const val ROW_SLOT_CHUNK_NUM_ROWS = 100
    }
}