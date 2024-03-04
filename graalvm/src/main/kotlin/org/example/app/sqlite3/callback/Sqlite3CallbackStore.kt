package org.example.app.sqlite3.callback

import java.util.Collections
import ru.pixnews.wasm.host.WasmPtr
import ru.pixnews.wasm.host.sqlite3.Sqlite3Db
import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback
import ru.pixnews.wasm.host.sqlite3.Sqlite3ProgressCallback
import ru.pixnews.wasm.host.sqlite3.Sqlite3TraceCallback

class Sqlite3CallbackStore {
    val sqlite3ExecCallbacks: IdMap<Sqlite3ExecCallbackId, Sqlite3ExecCallback> = IdMap(::Sqlite3ExecCallbackId)
    val sqlite3TraceCallbacks: MutableMap<WasmPtr<Sqlite3Db>, Sqlite3TraceCallback> = Collections.synchronizedMap(
        mutableMapOf()
    )
    val sqlite3ProgressCallbacks: MutableMap<WasmPtr<Sqlite3Db>, Sqlite3ProgressCallback> = Collections.synchronizedMap(
        mutableMapOf()
    )

    interface CallbackId {
        val id: Int
    }

    @JvmInline
    value class Sqlite3ExecCallbackId(override val id: Int) : CallbackId

    class IdMap<K: CallbackId, V: Any>(
        private val ctor: (Int) -> K
    ) {
        private val lock = Any()
        private var counter: Int = 1
        private val map: MutableMap<K, V> = mutableMapOf()

        fun put(value: V): K = synchronized(lock) {
            val newId = allocateId()
            map[newId] = value
            newId
        }

        operator fun get(key: K): V? = map[key]

        fun remove(key: K): V? = synchronized(lock) {
            map.remove(key)
        }

        private fun allocateId(): K {
            val start = counter
            var id = start
            while (map.containsKey(ctor(id))) {
                id = id.nextNonZeroId()
                if (id == start) {
                    throw RuntimeException("Can not allocate ID")
                }
            }
            counter = id.nextNonZeroId()
            return ctor(id)
        }

        private fun Int.nextNonZeroId(): Int {
            val nextId = this + 1
            return if (nextId != 0) nextId else 1
        }
    }
}