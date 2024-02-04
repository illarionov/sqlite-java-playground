package ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type

/**
 * A directory entry.
 *
 * @param dNext The offset of the next directory entry stored in this directory.
 * @param dIno The serial number of the file referred to by this directory entry.
 * @param dNamlen The length of the name of the directory entry.
 * @param dType The type of the file referred to by this directory entry.
 */
data class Dirent(
    val dNext: Dircookie, // (field $d_next $dircookie)
    val dIno: Inode, // (field $d_ino $inode)
    val dNamlen: Dirnamlen, // (field $d_namlen $dirnamlen)
    val dType: Filetype, // (field $d_type $filetype)
)