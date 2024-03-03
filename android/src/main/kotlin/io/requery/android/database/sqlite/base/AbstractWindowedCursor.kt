package io.requery.android.database.sqlite.base

import android.database.CharArrayBuffer
import android.database.Cursor
import android.database.StaleDataException
import io.requery.android.database.sqlite.internal.interop.NativeCursorWindow
import io.requery.android.database.sqlite.internal.interop.Sqlite3WindowPtr

/**
 * A base class for Cursors that store their data in [android.database.CursorWindow]s.
 *
 *
 * The cursor owns the cursor window it uses.  When the cursor is closed,
 * its window is also closed.  Likewise, when the window used by the cursor is
 * changed, its old window is closed.  This policy of strict ownership ensures
 * that cursor windows are not leaked.
 *
 *
 * Subclasses are responsible for filling the cursor window with data during
 * [.onMove], allocating a new cursor window if necessary.
 * During [.requery], the existing cursor window should be cleared and
 * filled with new data.
 *
 *
 * If the contents of the cursor change or become invalid, the old window must be closed
 * (because it is owned by the cursor) and set to null.
 *
 */
internal abstract class AbstractWindowedCursor<WP : Sqlite3WindowPtr>(
    private val windowCtor: (name: String?) -> CursorWindow<WP>,
) : AbstractCursor() {
    /**
     * The cursor window owned by this cursor.
     */
    protected var _window: CursorWindow<WP>? = null

    override fun getBlob(column: Int): ByteArray {
        checkPosition()
        return _window!!.getBlob(pos, column) ?: byteArrayOf()
    }

    override fun getString(column: Int): String? {
        checkPosition()
        return _window!!.getString(pos, column)
    }

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer) {
        _window!!.copyStringToBuffer(pos, columnIndex, buffer)
    }

    override fun getShort(column: Int): Short {
        checkPosition()
        return _window!!.getShort(pos, column)
    }

    override fun getInt(column: Int): Int {
        checkPosition()
        return _window!!.getInt(pos, column)
    }

    override fun getLong(column: Int): Long {
        checkPosition()
        return _window!!.getLong(pos, column)
    }

    override fun getFloat(column: Int): Float {
        checkPosition()
        return _window!!.getFloat(pos, column)
    }

    override fun getDouble(column: Int): Double {
        checkPosition()
        return _window!!.getDouble(pos, column)
    }

    override fun isNull(column: Int): Boolean {
        return _window!!.getType(pos, column) == NativeCursorWindow.CursorFieldType.NULL
    }

    override fun getType(column: Int): Int {
        return _window!!.getType(pos, column).id
    }

    override fun checkPosition() {
        super.checkPosition()
        if (_window == null) {
            throw StaleDataException(
                "Attempting to access a closed CursorWindow." +
                        "Most probable cause: cursor is deactivated prior to calling this method."
            )
        }
    }

    open var window: CursorWindow<WP>?
        get() = _window
        /**
         * Sets a new cursor window for the cursor to use.
         *
         *
         * The cursor takes ownership of the provided cursor window; the cursor window
         * will be closed when the cursor is closed or when the cursor adopts a new
         * cursor window.
         *
         *
         * If the cursor previously had a cursor window, then it is closed when the
         * new cursor window is assigned.
         *
         *
         * @param window The new cursor window, typically a remote cursor window.
         */
        set(window) {
            if (window !== _window) {
                closeWindow()
                _window = window
            }
        }

    /**
     * Returns true if the cursor has an associated cursor window.
     *
     * @return True if the cursor has an associated cursor window.
     */
    fun hasWindow(): Boolean = _window != null

    /**
     * Closes the cursor window and sets [.mWindow] to null.
     * @hide
     */
    protected fun closeWindow() {
        _window?.close()
        _window = null
    }

    /**
     * If there is a window, clear it. Otherwise, creates a new window.
     *
     * @param name The window name.
     * @hide
     */
    protected fun clearOrCreateWindow(name: String?) {
        _window?.clear() ?: run {
            _window = windowCtor(name)
        }
    }

    override fun onDeactivateOrClose() {
        super.onDeactivateOrClose()
        closeWindow()
    }
}