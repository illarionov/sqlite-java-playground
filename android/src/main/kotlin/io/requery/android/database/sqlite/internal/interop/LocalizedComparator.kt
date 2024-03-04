package io.requery.android.database.sqlite.internal.interop

import ru.pixnews.wasm.host.sqlite3.Sqlite3ComparatorCallback

class LocalizedComparator : Sqlite3ComparatorCallback {
    override fun invoke(a: String, b: String): Int {
        return a.compareTo(b) // TODO
    }
}