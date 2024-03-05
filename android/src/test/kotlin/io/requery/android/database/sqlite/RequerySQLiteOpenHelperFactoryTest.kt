package io.requery.android.database.sqlite

import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import co.touchlab.kermit.Logger
import io.requery.android.database.sqlite.internal.DatabasePathResolver
import io.requery.android.database.sqlite.internal.SQLiteDebug
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RequerySQLiteOpenHelperFactoryTest {
    val logger = Logger.withTag("RequerySQLiteOpenHelperFactoryTest")
    val mockContext = ContextWrapper(null)

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `Factory initialization should work`() {
        val helper = createHelper()
        helper.writableDatabase.use { db: SupportSQLiteDatabase ->
            logger.i { "db: $db; version: ${db.version}" }
            db.execSQL("CREATE TABLE IF NOT EXISTS User(id INTEGER PRIMARY KEY, name TEXT)")
            db.execSQL(
                "INSERT INTO User(`name`) VALUES (?), (?), (?)",
                arrayOf("user 1", "user 2", "user 3")
            )
            db.query("SELECT * FROM User").use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    logger.i { "$id: $name" }
                }
            }
        }
        println()
    }

    private fun createHelper(
        dbName: String = "test.db",
        openHelperCallback: SupportSQLiteOpenHelper.Callback = LoggingOpenHelperCallback(logger)
    ): SupportSQLiteOpenHelper {
        val pathResolver = DatabasePathResolver { name -> File(tempDir, name) }
        val debugConfig = SQLiteDebug(true, true, true, true)

        val factory = RequerySQLiteOpenHelperFactory(pathResolver, debugConfig)
        val config = SupportSQLiteOpenHelper.Configuration(mockContext, dbName, openHelperCallback)
        return factory.create(config)
    }

    private class LoggingOpenHelperCallback(
        private val logger: Logger = Logger.withTag("SupportSQLiteOpenHelperCallback"),
        version: Int = 1,
    ) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            logger.i { "onCreate() $db" }
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            logger.i { "onUpgrade() $db, $oldVersion, $newVersion" }
        }
    }
}