package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.StructStat
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

class FileSystem {
    
    public fun getCwd(): String {
        return "/"
    }

    fun stat(path: String): StructStat {
        throw SysException(Errno.BADF, "Not implemented for path `$path`")
    }
}