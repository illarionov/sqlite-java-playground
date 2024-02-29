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

import io.requery.android.database.sqlite.internal.SQLiteDatabase
import java.util.Locale
import java.util.regex.Pattern

/**
 * Describes how to configure a database.
 *
 * The purpose of this object is to keep track of all of the little
 * configuration settings that are applied to a database after it
 * is opened so that they can be applied to all connections in the
 * connection pool uniformly.
 *
 * Each connection maintains its own copy of this object so it can
 * keep track of which settings have already been applied.
 *
 */
public class SQLiteDatabaseConfiguration {
    /**
     * The database path.
     */
    val path: String

    /**
     * The label to use to describe the database when it appears in logs.
     * This is derived from the path but is stripped to remove PII.
     */
    val label: String

    /**
     * The flags used to open the database.
     */
    var openFlags: RequeryOpenFlags = RequeryOpenFlags.EMPTY

    /**
     * The maximum size of the prepared statement cache for each database connection.
     * Must be non-negative.
     *
     * Default is 25.
     */
    var maxSqlCacheSize: Int = 0

    /**
     * The database locale.
     *
     * Default is the value returned by [Locale.getDefault].
     */
    var locale: Locale? = null

    /**
     * True if foreign key constraints are enabled.
     *
     * Default is false.
     */
    var foreignKeyConstraintsEnabled: Boolean = false

    /**
     * Creates a database configuration with the required parameters for opening a
     * database and default values for all other parameters.
     *
     * @param path The database path.
     * @param openFlags Open flags for the database, such as [SQLiteDatabase.OPEN_READWRITE].
     */
    constructor(
        path: String = MEMORY_DB_PATH,
        openFlags: RequeryOpenFlags
    ) {
        this.path = path
        this.openFlags = openFlags
        label = stripPathForLogs(path)

        // Set default values for optional parameters.
        maxSqlCacheSize = 25
        locale = Locale.getDefault()
    }

    /**
     * Creates a database configuration as a copy of another configuration.
     *
     * @param other The other configuration.
     */
    internal constructor(other: SQLiteDatabaseConfiguration) {
        this.path = other.path
        this.label = other.label
        updateParametersFrom(other)
    }

    /**
     * Updates the non-immutable parameters of this configuration object
     * from the other configuration object.
     *
     * @param other The object from which to copy the parameters.
     */
    fun updateParametersFrom(other: SQLiteDatabaseConfiguration?) {
        requireNotNull(other) { "other must not be null." }
        require(path == other.path) {
            ("other configuration must refer to "
                    + "the same database.")
        }

        openFlags = other.openFlags
        maxSqlCacheSize = other.maxSqlCacheSize
        locale = other.locale
        foreignKeyConstraintsEnabled = other.foreignKeyConstraintsEnabled
    }

    val isInMemoryDb: Boolean
        /**
         * Returns true if the database is in-memory.
         * @return True if the database is in-memory.
         */
        get() = path.equals(MEMORY_DB_PATH, ignoreCase = true)

    companion object {
        // The pattern we use to strip email addresses from database paths
        // when constructing a label to use in log messages.
        private val EMAIL_IN_DB_PATTERN: Pattern = Pattern.compile("[\\w\\.\\-]+@[\\w\\.\\-]+")

        /**
         * Special path used by in-memory databases.
         */
        const val MEMORY_DB_PATH: String = ":memory:"

        private fun stripPathForLogs(path: String): String {
            if (path.indexOf('@') == -1) {
                return path
            }
            return EMAIL_IN_DB_PATTERN.matcher(path).replaceAll("XX@YY")
        }
    }
}