package io.requery.android.database.sqlite

import android.content.ContextWrapper
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import co.touchlab.kermit.Logger
import io.requery.android.database.sqlite.internal.DatabasePathResolver
import io.requery.android.database.sqlite.internal.SQLiteDebug
import io.requery.android.database.sqlite.room.AppDatabase1
import io.requery.android.database.sqlite.room.User
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
    }

    @Test
    fun `Test Room`() {
        val helperFactory = createHelperFactory()
        val db = Room.databaseBuilder(mockContext, AppDatabase1::class.java, "database-name")
            .openHelperFactory(helperFactory)
            .allowMainThreadQueries()
            .build()
        val userDao = db.userDao()

        val user101 = User(101, "User 101 First Name", "User 101 Last Name")
        userDao.insertAll(
            User(100, "User 100 First Name", "User 100 Last Name"),
            user101,
            User(102, "User 102 First Name", "User 102 Last Name"),
        )
        userDao.delete(user101)

        val usersByIds = userDao.loadAllByIds(intArrayOf(101, 102))
        val userByName = userDao.findByName("User 102 First Name", "User 102 Last Name")
        val users: List<User> = userDao.getAll()

        logger.i { "users by ids: $usersByIds; user by name: $userByName; users: $users;" }
    }

    private fun createHelper(
        dbName: String = "test.db",
        openHelperCallback: SupportSQLiteOpenHelper.Callback = LoggingOpenHelperCallback(logger)
    ): SupportSQLiteOpenHelper {
        val factory = createHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration(mockContext, dbName, openHelperCallback)
        return factory.create(config)
    }

    private fun createHelperFactory(): SupportSQLiteOpenHelper.Factory {
        val pathResolver = DatabasePathResolver { name -> File(tempDir, name) }
        val debugConfig = SQLiteDebug(true, true, true, true)

        return RequerySQLiteOpenHelperFactory(pathResolver, debugConfig)
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