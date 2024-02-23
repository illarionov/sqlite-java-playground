package org.example.app.sqlite3.callback

import ru.pixnews.wasm.host.sqlite3.Sqlite3ExecCallback

class Sqlite3CallbackStore {
    val sqlite3ExecCallbacks: IdMap<Sqlite3ExecCallbackId, Sqlite3ExecCallback> = IdMap(::Sqlite3ExecCallbackId)

    interface CallbackId {
        val id: Int
    }

    @JvmInline
    value class Sqlite3ExecCallbackId(
        override val id: Int
    ) : CallbackId

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
                id = id.nextIdNotZero()
                if (id == start) {
                    throw RuntimeException("Can not allocate ID")
                }
            }
            counter = id.nextIdNotZero()
            return ctor(id)
        }

        private fun Int.nextIdNotZero(): Int {
            val nextId = this + 1
            return if (nextId != 0) nextId else 1
        }
    }
}