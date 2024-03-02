package ru.pixnews.sqlite3.wasm

/**
 * Constants Defining Special Destructor Behavior
 *
 * https://www.sqlite.org/c3ref/c_static.html
 */
object Sqlite3DestructorType {

    /**
     * The content pointer is constant and will never change. It does not need to be destroyed
     */
    const val SQLITE_STATIC = 0

    /**
     * The content will likely change in the near future and SQLite should make its own private copy of the content
     * before returning.
     */
    const val SQLITE_TRANSIENT = 1
}