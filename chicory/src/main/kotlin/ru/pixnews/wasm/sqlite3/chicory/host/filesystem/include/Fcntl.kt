package ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include

public object Fcntl {
    const val O_RDONLY    = 0x0U
    const val O_WRONLY    = 0x1U
    const val O_RDWR      = 0x2U
    const val O_ACCMODE   = 0x3U

    const val O_CREAT     = 0x40U
    const val O_EXCL      = 0x80U
    const val O_NOCTTY    = 0x100U
    const val O_TRUNC     = 0x200U
    const val O_APPEND    = 0x400U
    const val O_NONBLOCK  = 0x800U
    const val O_NDELAY    = O_NONBLOCK
    const val O_DSYNC     = 0x1000U
    const val O_ASYNC     = 0x2000U
    const val O_DIRECT    = 0x4000U
    const val O_LARGEFILE = 0x8000U
    const val O_DIRECTORY = 0x10000U
    const val O_NOFOLLOW  = 0x20000U
    const val O_NOATIME   = 0x40000U
    const val O_CLOEXEC   = 0x80000U
    const val O_SYNC      = 0x101000U
    const val O_PATH      = 0x200000U
    const val O_TMPFILE   = 0x410000U
    const val O_SEARCH    = O_PATH

    fun oMaskToString(mask: UInt): String {
        var left = mask
        val names = mutableListOf<String>()
        if (mask.and(O_ACCMODE) == 0U) {
            names.add(::O_RDONLY.name)
        }

        listOf(
            ::O_WRONLY,
            ::O_RDWR,
            ::O_CREAT,
            ::O_EXCL,
            ::O_NOCTTY,
            ::O_TRUNC,
            ::O_APPEND,
            ::O_NONBLOCK,
            ::O_SYNC,
            ::O_TMPFILE,
            ::O_DSYNC,
            ::O_ASYNC,
            ::O_DIRECT,
            ::O_LARGEFILE,
            ::O_DIRECTORY,
            ::O_NOFOLLOW,
            ::O_NOATIME,
            ::O_CLOEXEC,
            ::O_PATH,
        ).forEach { prop ->
            val propMask: UInt = prop.get()
            if (left.and(propMask) != 0U) {
                names.add(prop.name)
                left = left.and(propMask.inv())
            }
        }
        return buildString {
            names.joinTo(this, ",")
            if (left != 0U) {
                append("+0x")
                append(left.toString(16))
            }
        }
    }
}