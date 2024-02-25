package io.requery.android.database.sqlite

import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * An object created from a SQLiteDatabase that can be closed.
 *
 * This class implements a primitive reference counting scheme for database objects.
 */
abstract class SQLiteClosable : Closeable {
    private var referenceCount = 1

    /**
     * Called when the last reference to the object was released by
     * a call to [.releaseReference] or [.close].
     */
    protected abstract fun onAllReferencesReleased()

    /**
     * Acquires a reference to the object.
     *
     * @throws IllegalStateException if the last reference to the object has already
     * been released.
     */
    fun acquireReference(): Unit = synchronized(this) {
        check(referenceCount > 0) { "attempt to re-open an already-closed object: $this" }
        referenceCount++
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * @see .onAllReferencesReleased
     */
    fun releaseReference() {
        var refCountIsZero: Boolean
        synchronized(this) {
            refCountIsZero = --referenceCount == 0
        }
        if (refCountIsZero) {
            onAllReferencesReleased()
        }
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * Calling this method is equivalent to calling [.releaseReference].
     *
     * @see .releaseReference
     * @see .onAllReferencesReleased
     */
    override fun close() {
        releaseReference()
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <R : Any?> SQLiteClosable.useReference(
    block: () -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    acquireReference()
    return try {
        block()
    } finally {
        releaseReference()
    }
}