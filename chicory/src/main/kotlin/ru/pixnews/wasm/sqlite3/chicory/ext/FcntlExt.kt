package ru.pixnews.wasm.sqlite3.chicory.ext

import kotlin.reflect.KProperty0
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.Fcntl

internal fun Fcntl.oMaskToString(mask: UInt): String {
    val startNames = if (mask.and(Fcntl.O_ACCMODE) == 0U) {
        listOf(Fcntl::O_RDONLY.name)
    } else {
        emptyList()
    }

    return maskToString(
        mask,
        listOf(
            Fcntl::O_WRONLY,
            Fcntl::O_RDWR,
            Fcntl::O_CREAT,
            Fcntl::O_EXCL,
            Fcntl::O_NOCTTY,
            Fcntl::O_TRUNC,
            Fcntl::O_APPEND,
            Fcntl::O_NONBLOCK,
            Fcntl::O_SYNC,
            Fcntl::O_TMPFILE,
            Fcntl::O_DSYNC,
            Fcntl::O_ASYNC,
            Fcntl::O_DIRECT,
            Fcntl::O_LARGEFILE,
            Fcntl::O_DIRECTORY,
            Fcntl::O_NOFOLLOW,
            Fcntl::O_NOATIME,
            Fcntl::O_CLOEXEC,
            Fcntl::O_PATH,
        ),
        startNames
    )
}

internal fun Fcntl.sMaskToString(mask: UInt): String = "0" + mask.toString(8)

internal fun Fcntl.sMaskToStringLong(mask: UInt): String = maskToString(mask, listOf(
    Fcntl::S_ISUID,
    Fcntl::S_ISGID,
    Fcntl::S_ISVTX,
    Fcntl::S_IRUSR,
    Fcntl::S_IWUSR,
    Fcntl::S_IXUSR,
    Fcntl::S_IRWXU,
    Fcntl::S_IRGRP,
    Fcntl::S_IWGRP,
    Fcntl::S_IXGRP,
    Fcntl::S_IRWXG,
    Fcntl::S_IROTH,
    Fcntl::S_IWOTH,
    Fcntl::S_IXOTH,
    Fcntl::S_IRWXO,
))

private fun maskToString(
    mask: UInt,
    maskProps: List<KProperty0<UInt>>,
    startNames: List<String> = emptyList(),
): String {
    var left = mask
    val names = startNames.toMutableList()
    maskProps.forEach { prop: KProperty0<UInt> ->
        val propMask: UInt = prop.get()
        if (left.and(propMask) != 0U) {
            names.add(prop.name)
            left = left.and(propMask.inv())
        }
    }
    return buildString {
        names.joinTo(this, ",")
        if (left != 0U) {
            append("0")
            append(left.toString(8))
        }
    }
}