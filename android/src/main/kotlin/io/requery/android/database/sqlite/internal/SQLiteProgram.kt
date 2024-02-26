package io.requery.android.database.sqlite.internal

import androidx.core.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteProgram

/**
 * A base class for compiled SQLite programs.
 *
 *
 * This class is not thread-safe.
 *
 */
internal abstract class SQLiteProgram internal constructor(
    val database: SQLiteDatabase,
    sql: String,
    bindArgs: List<Any?>,
    cancellationSignalForPrepare: CancellationSignal?
) : SQLiteClosable(), SupportSQLiteProgram {
    val sql: String = sql.trim { it <= ' ' }
    val columnNames: List<String>
    private val readOnly: Boolean
    private val numParameters: Int
    private val _bindArgs: MutableList<Any?>
    val bindArgs: List<Any?> get() = _bindArgs

    init {
        when (val n = SQLiteStatementType.getSqlStatementType(this.sql)) {
            SQLiteStatementType.STATEMENT_BEGIN, SQLiteStatementType.STATEMENT_COMMIT, SQLiteStatementType.STATEMENT_ABORT -> {
                readOnly = false
                columnNames = listOf()
                numParameters = 0
            }

            else -> {
                val assumeReadOnly = (n == SQLiteStatementType.STATEMENT_SELECT)
                val info = database.threadSession.prepare(
                    this.sql,
                    database.getThreadDefaultConnectionFlags(assumeReadOnly),
                    cancellationSignalForPrepare
                )
                readOnly = info.readOnly
                columnNames = info.columnNames
                numParameters = info.numParameters
            }
        }

        require(bindArgs.size <= numParameters) {
            ("Too many bind arguments. ${bindArgs.size} arguments were provided but the statement needs $numParameters arguments.")
        }

        this._bindArgs = MutableList(numParameters) { null }
        bindArgs.forEachIndexed { index, value -> _bindArgs[index] = value }
    }

    protected val session: SQLiteSession
        get() = database.threadSession

    protected val connectionFlags: Int
        get() = database.getThreadDefaultConnectionFlags(readOnly)

    protected fun onCorruption(): Unit = database.onCorruption()

    override fun bindNull(index: Int) = bind(index, null)

    override fun bindLong(index: Int, value: Long) = bind(index, value)

    override fun bindDouble(index: Int, value: Double) = bind(index, value)

    override fun bindString(index: Int, value: String) = bind(index, value)

    override fun bindBlob(index: Int, value: ByteArray) = bind(index, value)

    override fun clearBindings() = _bindArgs.indices.forEach { _bindArgs[it] = null }

    /**
     * Given an array of String bindArgs, this method binds all of them in one single call.
     *
     * @param bindArgs the String array of bind args, none of which must be null.
     */
    fun bindAllArgsAsStrings(bindArgs: List<String?>) {
        (bindArgs.size downTo 1).forEach { i ->
            val arg = bindArgs[i - 1]
            if (arg != null) {
                bindString(i, arg)
            } else {
                bindNull(i)
            }
        }
    }

    override fun onAllReferencesReleased() = clearBindings()

    private fun bind(index: Int, value: Any?) {
        require(index in 1..numParameters) {
            ("Cannot bind argument at index "
                    + index + " because the index is out of range.  "
                    + "The statement has " + numParameters + " parameters.")
        }
        _bindArgs[index - 1] = value
    }
}