package io.requery.android.database.sqlite.internal.interop

import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.sqlite3.Sqlite3ComparatorCallbackRaw

class LocalizedComparator : Sqlite3ComparatorCallbackRaw {
    override fun invoke(aLength: Int, aPtr: WasmPtr<Byte>, bLength: Int, bPtr: WasmPtr<Byte>): Int {
        TODO("Not yet implemented")
    }
}