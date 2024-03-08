package ru.pixnews.gradle.wasm.sqlite3

import java.io.Serializable
import org.gradle.api.NamedDomainObjectContainer

public abstract class WasmSqlite3Extension : Serializable {

    public abstract val builds: NamedDomainObjectContainer<Sqlite3WasmBuildSpec>

    public companion object {
        private const val serialVersionUID: Long = -1
    }
}