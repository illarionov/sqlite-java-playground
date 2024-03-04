/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.requery.android.database.sqlite

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.requery.android.database.sqlite.base.DatabaseErrorHandler
import io.requery.android.database.sqlite.internal.RequerySqliteOpenHelper
import io.requery.android.database.sqlite.internal.SQLiteDatabase
import io.requery.android.database.sqlite.internal.interop.GraalNativeBindings
import io.requery.android.database.sqlite.internal.interop.GraalWindowBindings
import io.requery.android.database.sqlite.internal.interop.SqlOpenHelperNativeBindings
import io.requery.android.database.sqlite.internal.interop.SqlOpenHelperWindowBindings
import io.requery.android.database.sqlite.internal.interop.Sqlite3ConnectionPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3StatementPtr
import io.requery.android.database.sqlite.internal.interop.Sqlite3WindowPtr
import org.example.app.sqlite3.Sqlite3CApi

/**
 * Implements [SupportSQLiteOpenHelper.Factory] using the SQLite implementation shipped in
 * this library.
 */
public class RequerySQLiteOpenHelperFactory(
    private val configurationOptions: List<ConfigurationOptions> = emptyList()
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val api = Sqlite3CApi()
        val bindings = GraalNativeBindings(api)
        val windowBindins = GraalWindowBindings()

        return CallbackSQLiteOpenHelper(
            configuration.context,
            configuration.name,
            configuration.callback,
            configurationOptions,
            bindings,
            windowBindins,
        )
    }

    private class CallbackSQLiteOpenHelper<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr>(
        context: Context,
        name: String?,
        cb: SupportSQLiteOpenHelper.Callback,
        ops: Iterable<ConfigurationOptions>,
        bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
        windowBindings: SqlOpenHelperWindowBindings<WP>,
    ) : RequerySqliteOpenHelper<CP, SP, WP>(
        context = context,
        databaseName = name,
        factory = null,
        version = cb.version,
        errorHandler = CallbackDatabaseErrorHandler(cb),
        bindings, windowBindings
    ) {
        private val callback: SupportSQLiteOpenHelper.Callback = cb
        private val configurationOptions = ops

        override fun onConfigure(db: SQLiteDatabase<CP, SP, WP>) = callback.onConfigure(db)

        override fun onCreate(db: SQLiteDatabase<CP, SP, WP>) = callback.onCreate(db)

        override fun onUpgrade(db: SQLiteDatabase<CP, SP, WP>, oldVersion: Int, newVersion: Int) =
            callback.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SQLiteDatabase<CP, SP, WP>, oldVersion: Int, newVersion: Int): Unit =
            callback.onDowngrade(db, oldVersion, newVersion)

        override fun onOpen(db: SQLiteDatabase<CP, SP, WP>) = callback.onOpen(db)

        override fun createConfiguration(path: String, openFlags: RequeryOpenFlags): SQLiteDatabaseConfiguration {
            var config = super.createConfiguration(path, openFlags)

            configurationOptions.forEach { option ->
                config = option.apply(config)
            }

            return config
        }
    }

    private class CallbackDatabaseErrorHandler(
        private val callback: SupportSQLiteOpenHelper.Callback
    ) : DatabaseErrorHandler {
        override fun onCorruption(dbObj: SQLiteDatabase<*, *, *>) = callback.onCorruption(dbObj)
    }

    fun interface ConfigurationOptions {
        fun apply(configuration: SQLiteDatabaseConfiguration): SQLiteDatabaseConfiguration
    }
}