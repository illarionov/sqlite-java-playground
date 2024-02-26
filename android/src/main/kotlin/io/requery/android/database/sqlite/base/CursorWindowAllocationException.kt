package io.requery.android.database.sqlite.base

/**
 * This exception is thrown when a CursorWindow couldn't be allocated,
 * most probably due to memory not being available.
 *
 * @hide
 */
class CursorWindowAllocationException(description: String?) : RuntimeException(description)