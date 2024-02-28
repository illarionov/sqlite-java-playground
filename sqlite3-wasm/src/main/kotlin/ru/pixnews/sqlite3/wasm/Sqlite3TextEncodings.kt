package ru.pixnews.sqlite3.wasm

enum class Sqlite3TextEncodings(
    val id: Int
) {
   SQLITE_UTF8(1),
   SQLITE_UTF16LE(2),
   SQLITE_UTF16BE(3),
   SQLITE_UTF16(4),
   SQLITE_ANY(5), // Deprecated
   SQLITE_UTF16_ALIGNED(8)    /* sqlite3_create_collation only */

   ;

   companion object {
      val entriesMap: Map<Int, Sqlite3TextEncodings> = Sqlite3TextEncodings.entries.associateBy(Sqlite3TextEncodings::id)
   }
}