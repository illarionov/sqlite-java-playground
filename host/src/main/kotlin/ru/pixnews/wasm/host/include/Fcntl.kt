package ru.pixnews.wasm.host.include

object Fcntl {
    const val O_RDONLY = 0x0U
    const val O_WRONLY = 0x1U
    const val O_RDWR = 0x2U
    const val O_ACCMODE = 0x3U

    const val O_CREAT = 0x40U
    const val O_EXCL = 0x80U
    const val O_NOCTTY = 0x100U
    const val O_TRUNC = 0x200U
    const val O_APPEND = 0x400U
    const val O_NONBLOCK = 0x800U
    const val O_NDELAY = O_NONBLOCK
    const val O_DSYNC = 0x1000U
    const val O_ASYNC = 0x2000U
    const val O_DIRECT = 0x4000U
    const val O_LARGEFILE = 0x8000U
    const val O_DIRECTORY = 0x10000U
    const val O_NOFOLLOW = 0x20000U
    const val O_NOATIME = 0x40000U
    const val O_CLOEXEC = 0x80000U
    const val O_SYNC = 0x101000U
    const val O_PATH = 0x200000U
    const val O_TMPFILE = 0x410000U
    const val O_SEARCH = O_PATH

    const val S_ISUID = 0x800U
    const val S_ISGID = 0x400U
    const val S_ISVTX = 0x200U
    const val S_IRUSR = 0x100U
    const val S_IWUSR = 0x80U
    const val S_IXUSR = 0x40U
    const val S_IRWXU = 0x1c0U
    const val S_IRGRP = 0x20U
    const val S_IWGRP = 0x10U
    const val S_IXGRP = 0x08U
    const val S_IRWXG = 0x38U
    const val S_IROTH = 0x04U
    const val S_IWOTH = 0x02U
    const val S_IXOTH = 0x01U
    const val S_IRWXO = 0x07U

    const val AT_FDCWD = -100
    const val AT_SYMLINK_NOFOLLOW = 0x100U
    const val AT_REMOVEDIR = 0x200U
    const val AT_SYMLINK_FOLLOW = 0x400U
    const val AT_EACCESS = 0x200U
}