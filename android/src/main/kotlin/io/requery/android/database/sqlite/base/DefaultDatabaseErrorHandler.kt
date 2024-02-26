package io.requery.android.database.sqlite.base

import android.database.sqlite.SQLiteException
import android.util.Log
import android.util.Pair
import io.requery.android.database.sqlite.internal.SQLiteDatabase
import java.io.File

/**
 * Default class used to define the actions to take when the database corruption is reported
 * by sqlite.
 *
 *
 * An application can specify an implementation of [DatabaseErrorHandler] on the
 * following:
 *
 *  * [SQLiteDatabase.openOrCreateDatabase]
 *  * [SQLiteDatabase.openDatabase]
 *
 * The specified [DatabaseErrorHandler] is used to handle database corruption errors, if they
 * occur.
 *
 *
 * If null is specified for DatabaeErrorHandler param in the above calls, then this class is used
 * as the default [DatabaseErrorHandler].
 */
internal class DefaultDatabaseErrorHandler : DatabaseErrorHandler {
    override fun onCorruption(dbObj: SQLiteDatabase) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.path)

        // is the corruption detected even before database could be 'opened'?
        if (!dbObj.isOpen) {
            // database files are not even openable. delete this database file.
            // NOTE if the database has attached databases, then any of them could be corrupt.
            // and not deleting all of them could cause corrupted database file to remain and
            // make the application crash on database open operation. To avoid this problem,
            // the application should provide its own {@link DatabaseErrorHandler} impl class
            // to delete ALL files of the database (including the attached databases).
            dbObj.path?.let(::deleteDatabaseFile)
            return
        }

        var attachedDbs: List<Pair<String, String>>? = null
        try {
            // Close the database, which will cause subsequent operations to fail.
            // before that, get the attached database list first.
            try {
                attachedDbs = dbObj.attachedDbs
            } catch (e: SQLiteException) {
                /* ignore */
            }
            try {
                dbObj.close()
            } catch (e: SQLiteException) {
                /* ignore */
            }
        } finally {
            // Delete all files of this corrupt database and/or attached databases
            if (attachedDbs != null) {
                for (p in attachedDbs) {
                    deleteDatabaseFile(p.second)
                }
            } else {
                // attachedDbs = null is possible when the database is so corrupt that even
                // "PRAGMA database_list;" also fails. delete the main database file
                dbObj.path?.let(::deleteDatabaseFile)
            }
        }
    }

    private fun deleteDatabaseFile(fileName: String) {
        if (fileName.equals(":memory:", ignoreCase = true) || fileName.trim { it <= ' ' }.isEmpty()) {
            return
        }
        Log.e(TAG, "deleting the database file: $fileName")
        try {
            SQLiteDatabase.deleteDatabase(File(fileName))
        } catch (e: Exception) {
            /* print warning and ignore exception */
            Log.w(TAG, "delete failed: " + e.message)
        }
    }

    companion object {
        private const val TAG = "DefaultDatabaseError"
    }
}