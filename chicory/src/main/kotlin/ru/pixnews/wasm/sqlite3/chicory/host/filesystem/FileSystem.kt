package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.readAttributes
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.StructTimespec
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.StructStat
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.blkcnt_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.blksize_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.dev_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.gid_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.ino_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.mode_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.nlink_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.off_t
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.sys.uid_t
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

class FileSystem(
    private val javaFs: FileSystem = FileSystems.getDefault()
) {

    fun getCwd(): String {
        val cwd = javaFs.getPath("").toAbsolutePath().toString();
        return cwd
    }

    fun stat(
        path: String,
        followSymlinks: Boolean = true,
    ): StructStat {
        val linkOptions: Array<LinkOption> = if (followSymlinks) {
            arrayOf()
        } else {
            arrayOf(LinkOption.NOFOLLOW_LINKS)
        }

        val filePath = javaFs.getPath(path)

        if (!filePath.exists(options = linkOptions)) {
            throw SysException(Errno.NOENT)
        }

        val basicFileAttrs = try {
            filePath.readAttributes<BasicFileAttributes>(options = linkOptions)
        } catch (e: UnsupportedOperationException) {
            throw SysException(Errno.PERM, "Can not get BasicFileAttributeView", e)
        } catch (e: IOException) {
            throw SysException(Errno.PERM, "Can not read attributes", e)
        } catch (e: SecurityException) {
            throw SysException(Errno.PERM, "Can not read attributes", e)
        }

        val unixAttrs: Map<String, Any?> = try {
            filePath.readAttributes(UNIX_REQUESTED_ATTRIBUTES, options = linkOptions)
        } catch (e: Throwable) {
            when (e) {
                is UnsupportedOperationException, is IOException, is SecurityException -> emptyMap()
                else -> throw e
            }
        }

        val dev: dev_t = (unixAttrs[ATTR_UNI_DEV] as? Long)?.toULong() ?: 1UL
        val ino: ino_t = (unixAttrs[ATTR_UNI_INO] as? Long)?.toULong()
            ?: basicFileAttrs.fileKey().hashCode().toULong()
        val mode: mode_t = getMode(basicFileAttrs, unixAttrs)
        val nlink: nlink_t = (unixAttrs[ATTR_UNI_NLINK] as? Int)?.toULong() ?: 1UL
        val uid: uid_t = (unixAttrs[ATTR_UNI_UID] as? Int)?.toULong() ?: 0UL
        val gid: gid_t = (unixAttrs[ATTR_UNI_GID] as? Int)?.toULong() ?: 0UL
        val rdev: dev_t = (unixAttrs[ATTR_UNI_RDEV] as? Long)?.toULong() ?: 1UL
        val size: off_t = basicFileAttrs.size().toULong()
        val blksize: blksize_t = 4096UL;
        val blocks: blkcnt_t = (size + blksize - 1UL) / blksize
        val mtim: StructTimespec = basicFileAttrs.lastModifiedTime().toTimeSpec()

        val cTimeFileTime = unixAttrs[ATTR_UNI_CTIME] ?: basicFileAttrs.creationTime()
        val ctim: StructTimespec = checkNotNull(cTimeFileTime as? FileTime) {
            "Can not get file creation time"
        }.toTimeSpec()
        val atim: StructTimespec = basicFileAttrs.lastAccessTime().toTimeSpec()

        return StructStat(
            st_dev = dev,
            st_ino = ino,
            st_mode = mode,
            st_nlink = nlink,
            st_uid = uid,
            st_gid = gid,
            st_rdev = rdev,
            st_size = size,
            st_blksize = blksize,
            st_blocks = blocks,
            st_atim = atim,
            st_mtim = mtim,
            st_ctim = ctim,
        )
    }

    private companion object {
        private const val ATTR_UNI_CTIME = "ctime"
        private const val ATTR_UNI_DEV = "dev"
        private const val ATTR_UNI_GID = "gid"
        private const val ATTR_UNI_INO = "ino"
        private const val ATTR_UNI_MODE = "mode"
        private const val ATTR_UNI_NLINK = "nlink"
        private const val ATTR_UNI_RDEV = "rdev"
        private const val ATTR_UNI_UID = "uid"

        private val UNIX_REQUESTED_ATTRIBUTES = "unix:" +
                listOf(
                    ATTR_UNI_DEV,
                    ATTR_UNI_INO,
                    ATTR_UNI_MODE,
                    ATTR_UNI_NLINK,
                    ATTR_UNI_UID,
                    ATTR_UNI_GID,
                    ATTR_UNI_RDEV,
                    ATTR_UNI_CTIME,
                ).joinToString(",")

        private fun getMode(
            basicAttrs: BasicFileAttributes,
            unixAttrs: Map<String, Any?>
        ): mode_t {
            val unixMode = unixAttrs[ATTR_UNI_MODE] as? Int
            if (unixMode != null) {
                return unixMode.toULong()
            }

            // TODO: guess from Basic mode?

            return "777".toULong(radix = 8)
        }

        private fun FileTime.toTimeSpec(): StructTimespec = toInstant().run {
            StructTimespec(
                tv_sec = epochSecond.toULong(),
                tv_nsec = nano.toULong()
            )
        }
    }
}