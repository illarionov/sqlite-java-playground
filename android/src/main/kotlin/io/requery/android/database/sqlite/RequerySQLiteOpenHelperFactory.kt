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
import io.requery.android.database.DatabaseErrorHandler

/**
 * Implements [SupportSQLiteOpenHelper.Factory] using the SQLite implementation shipped in
 * this library.
 */
public class RequerySQLiteOpenHelperFactory(
    private val configurationOptions: List<ConfigurationOptions> = emptyList()
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return CallbackSQLiteOpenHelper(
            configuration.context,
            configuration.name,
            configuration.callback,
            configurationOptions
        )
    }

    private class CallbackSQLiteOpenHelper(
        context: Context,
        name: String?,
        cb: SupportSQLiteOpenHelper.Callback,
        ops: Iterable<ConfigurationOptions>
    ) : SQLiteOpenHelper(
        /* context = */ context,
        /* name = */ name,
        /* factory = */ null,
        /* version = */ cb.version,
        /* errorHandler = */ CallbackDatabaseErrorHandler(cb)
    ) {
        private val callback: SupportSQLiteOpenHelper.Callback = cb
        private val configurationOptions = ops

        override fun onConfigure(db: SQLiteDatabase) = callback.onConfigure(db)

        override fun onCreate(db: SQLiteDatabase) = callback.onCreate(db)

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
            callback.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit =
            callback.onDowngrade(db, oldVersion, newVersion)

        override fun onOpen(db: SQLiteDatabase) = callback.onOpen(db)

        override fun createConfiguration(path: String, openFlags: Int): SQLiteDatabaseConfiguration {
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
        override fun onCorruption(db: SQLiteDatabase) = callback.onCorruption(db)
    }

    interface ConfigurationOptions {
        fun apply(configuration: SQLiteDatabaseConfiguration?): SQLiteDatabaseConfiguration
    }
}