package ru.pixnews.wasm.sqlite3.chicory.host.filesystem

import com.sun.nio.file.ExtendedOpenOption
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.include.Fcntl
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
import ru.pixnews.wasm.sqlite3.chicory.host.filesystem.model.FdChannel
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

class FileSystem(
    internal val javaFs: FileSystem = FileSystems.getDefault(),
    private val logger: Logger = Logger.getLogger(ru.pixnews.wasm.sqlite3.chicory.host.filesystem.FileSystem::class.qualifiedName)
) {
    private val fileDescriptors: FileDescriptorMap = FileDescriptorMap(this)

    public fun getCwd(): String = getCwdPath().pathString

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

    fun open(
        path: Path,
        flags: UInt,
        mode: UInt
    ): FdChannel {
        if (path.pathString.isEmpty()) {
            throw SysException(Errno.NOENT)
        }

        val channel = try {
            val openOptions = getOpenOptions(flags)
            val fileAttributes = getOpenFileAttributes(mode)
            logger.finest { "open(): path: `$path`, options: `${openOptions}, attrs: ${fileAttributes.value()}`" }
            FileChannel.open(
                path,
                openOptions,
                fileAttributes,
            )
        } catch (iae: IllegalArgumentException) {
            throw SysException(Errno.INVAL)
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(Errno.INVAL)
        } catch (fae: FileAlreadyExistsException) {
            throw SysException(Errno.EXIST)
        } catch (ioe: IOException) {
            throw SysException(Errno.IO)
        } catch (se: SecurityException) {
            throw SysException(Errno.PERM)
        }

        val fd = fileDescriptors.add(path, channel)
        return fd
    }

    internal fun getCwdPath(): Path {
        return javaFs.getPath("").toAbsolutePath()
    }

    internal fun getPathByFd(fd: Int): Path {
        throw SysException(Errno.NOSYS, "Not implemented")
    }

    private fun getOpenOptions(
        flags: UInt,
    ): Set<OpenOption> {
        val options = mutableSetOf<OpenOption>()
        if (flags and Fcntl.O_WRONLY != 0U) {
            options += StandardOpenOption.WRITE
        } else if (flags and Fcntl.O_RDWR != 0U) {
            options += StandardOpenOption.READ
            options += StandardOpenOption.WRITE
        }

        if (flags and Fcntl.O_APPEND != 0U) {
            options += StandardOpenOption.APPEND
        }

        if (flags and Fcntl.O_CREAT != 0U) {
            options += if (flags and Fcntl.O_EXCL != 0U) {
                StandardOpenOption.CREATE_NEW
            } else {
                StandardOpenOption.CREATE
            }
        }

        if (flags and Fcntl.O_TRUNC != 0U) {
            options += StandardOpenOption.TRUNCATE_EXISTING
        }

        if (flags and Fcntl.O_NONBLOCK != 0U) {
            logger.info { "O_NONBLOCK" + " not implemented" }
        }

        if (flags and Fcntl.O_ASYNC != 0U) {
            logger.info { "O_ASYNC" + " not implemented" }
        }

        if (flags and (Fcntl.O_DSYNC or Fcntl.O_SYNC) != 0U) {
            options += StandardOpenOption.SYNC
        }

        if (flags and Fcntl.O_DIRECT != 0U) {
            options += ExtendedOpenOption.DIRECT
        }

        if (flags and Fcntl.O_DIRECTORY != 0U) {
            throw SysException(Errno.ISDIR, "O_DIRECTORY" + " not implemented")
        }

        if (flags and Fcntl.O_NOFOLLOW != 0U) {
            options += LinkOption.NOFOLLOW_LINKS
        }
        if (flags and Fcntl.O_NOATIME != 0U) {
            logger.info { "O_NOATIME not implemented" }
        }
        if (flags and Fcntl.O_CLOEXEC != 0U) {
            logger.info { "O_CLOEXEC not implemented" }
        }

        if (flags and Fcntl.O_PATH != 0U) {
            throw SysException(Errno.ISDIR, "O_PATH" + " not implemented")
        }

        if (flags and Fcntl.O_TMPFILE != 0U) {
            logger.info { "O_TMPFILE not implemented" }
            options += StandardOpenOption.DELETE_ON_CLOSE
        }

        return options
    }

    private fun getOpenFileAttributes(
        mode: UInt,
    ): FileAttribute<Set<PosixFilePermission>> {
        val permissions = mutableSetOf<PosixFilePermission>()

        if (mode and Fcntl.S_IRUSR != 0U) permissions += PosixFilePermission.OWNER_READ
        if (mode and Fcntl.S_IWUSR != 0U) permissions += PosixFilePermission.OWNER_WRITE
        if (mode and Fcntl.S_IXUSR != 0U) permissions += PosixFilePermission.OWNER_EXECUTE

        if (mode and Fcntl.S_IRGRP != 0U) permissions += PosixFilePermission.GROUP_READ
        if (mode and Fcntl.S_IWGRP != 0U) permissions += PosixFilePermission.GROUP_WRITE
        if (mode and Fcntl.S_IXGRP != 0U) permissions += PosixFilePermission.GROUP_EXECUTE

        if (mode and Fcntl.S_IROTH != 0U) permissions += PosixFilePermission.OTHERS_READ
        if (mode and Fcntl.S_IWOTH != 0U) permissions += PosixFilePermission.OTHERS_WRITE
        if (mode and Fcntl.S_IXOTH != 0U) permissions += PosixFilePermission.OTHERS_EXECUTE

        mode.and(SUPPORTED_MODES.inv()).let {
            if (it != 0U) {
                logger.info { "Mode 0${it.toString(8)} not supported" }
            }
        }

        return PosixFilePermissions.asFileAttribute(permissions)
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

        private val SUPPORTED_MODES = Fcntl.S_IRUSR or Fcntl.S_IWUSR or Fcntl.S_IXUSR or
                Fcntl.S_IRGRP or Fcntl.S_IWGRP or Fcntl.S_IXGRP or
                Fcntl.S_IROTH or Fcntl.S_IWOTH or Fcntl.S_IXOTH

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