package io.requery.android.database.sqlite.internal

import android.os.SystemClock
import android.util.Log
import android.util.Printer
import androidx.core.os.CancellationSignal
import androidx.core.os.OperationCanceledException
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration
import java.io.Closeable
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport

/**
 * Maintains a pool of active SQLite database connections.
 *
 * At any given time, a connection is either owned by the pool, or it has been
 * acquired by a [SQLiteSession].  When the [SQLiteSession] is
 * finished with the connection it is using, it must return the connection
 * back to the pool.
 *
 *
 * The pool holds strong references to the connections it owns.  However,
 * it only holds *weak references* to the connections that sessions
 * have acquired from it.  Using weak references in the latter case ensures
 * that the connection pool can detect when connections have been improperly
 * abandoned so that it can create new connections to replace them if needed.
 *
 * The connection pool is thread-safe (but the connections themselves are not).
 *
 * <h2>Exception safety</h2>
 *
 * This code attempts to maintain the invariant that opened connections are
 * always owned.  Unfortunately that means it needs to handle exceptions
 * all over to ensure that broken connections get cleaned up.  Most
 * operations invokving SQLite can throw [SQLiteException] or other
 * runtime exceptions.  This is a bit of a pain to deal with because the compiler
 * cannot help us catch missing exception handling code.
 *
 * The general rule for this file: If we are making calls out to
 * [SQLiteConnection] then we must be prepared to handle any
 * runtime exceptions it might throw at us.  Note that out-of-memory
 * is an [Error], not a [RuntimeException].  We don't trouble ourselves
 * handling out of memory because it is hard to do anything at all sensible then
 * and most likely the VM is about to crash.
 *
 */
internal class SQLiteConnectionPool private constructor(
    configuration: SQLiteDatabaseConfiguration
) : Closeable {
    private val closeGuard: CloseGuard = CloseGuard.get()

    private val lock = Any()
    private val connectionLeaked = AtomicBoolean()
    private val configuration = SQLiteDatabaseConfiguration(configuration)
    private var maxConnectionPoolSize = 0
    private var isOpen = false
    private var nextConnectionId = 0

    private var connectionWaiterPool: ConnectionWaiter? = null
    private var connectionWaiterQueue: ConnectionWaiter? = null

    // Strong references to all available connections.
    private val availableNonPrimaryConnections = mutableListOf<SQLiteConnection>()
    private var availablePrimaryConnection: SQLiteConnection? = null

    // Describes what should happen to an acquired connection when it is returned to the pool.
    internal enum class AcquiredConnectionStatus {
        // The connection should be returned to the pool as usual.
        NORMAL,

        // The connection must be reconfigured before being returned.
        RECONFIGURE,

        // The connection must be closed and discarded.
        DISCARD,
    }

    // Weak references to all acquired connections.  The associated value
    // indicates whether the connection must be reconfigured before being
    // returned to the available connection list or discarded.
    // For example, the prepared statement cache size may have changed and
    // need to be updated in preparation for the next client.
    private val acquiredConnections = WeakHashMap<SQLiteConnection, AcquiredConnectionStatus>()

    init {
        setMaxConnectionPoolSizeLocked()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        dispose(true)
    }

    // Might throw
    private fun open() {
        // Open the primary connection.
        // This might throw if the database is corrupt.
        availablePrimaryConnection = openConnectionLocked(
            configuration = configuration,
            primaryConnection = true
        ) // might throw

        // Mark the pool as being open for business.
        isOpen = true
        closeGuard.open("close")
    }

    /**
     * Closes the connection pool.
     *
     * When the connection pool is closed, it will refuse all further requests
     * to acquire connections.  All connections that are currently available in
     * the pool are closed immediately.  Any connections that are still in use
     * will be closed as soon as they are returned to the pool.
     *
     * @throws IllegalStateException if the pool has been closed.
     */
    override fun close() {
        dispose(false)
    }

    private fun dispose(finalized: Boolean) {
        if (finalized) {
            closeGuard.warnIfOpen()
        }
        closeGuard.close()

        if (!finalized) {
            // Close all connections.  We don't need (or want) to do this
            // when finalized because we don't know what state the connections
            // themselves will be in.  The finalizer is really just here for CloseGuard.
            // The connections will take care of themselves when their own finalizers run.
            synchronized(lock) {
                throwIfClosedLocked()
                isOpen = false

                closeAvailableConnectionsAndLogExceptionsLocked()

                val pendingCount = acquiredConnections.size
                if (pendingCount != 0) {
                    Log.i(
                        TAG,
                        "The connection pool for ${configuration.label} has been closed but there are still " +
                                "$pendingCount connections in use.  They will be closed as they are released back to " +
                                "the pool."
                    )
                }
                wakeConnectionWaitersLocked()
            }
        }
    }

    /**
     * Reconfigures the database configuration of the connection pool and all of its
     * connections.
     *
     * Configuration changes are propagated down to connections immediately if
     * they are available or as soon as they are released.  This includes changes
     * that affect the size of the pool.
     *
     *
     * @param configuration The new configuration.
     *
     * @throws IllegalStateException if the pool has been closed.
     */
    fun reconfigure(configuration: SQLiteDatabaseConfiguration): Unit = synchronized(lock) {
        throwIfClosedLocked()
        val walModeChanged = (
                (configuration.openFlags xor this.configuration.openFlags) and SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
                ) != 0
        if (walModeChanged) {
            // WAL mode can only be changed if there are no acquired connections
            // because we need to close all but the primary connection first.
            if (!acquiredConnections.isEmpty()) {
                throw IllegalStateException(
                    "Write Ahead Logging (WAL) mode cannot "
                            + "be enabled or disabled while there are transactions in "
                            + "progress.  Finish all transactions and release all active "
                            + "database connections first."
                )
            }

            // Close all non-primary connections.  This should happen immediately
            // because none of them are in use.
            closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked()
            assert(availableNonPrimaryConnections.isEmpty())
        }

        val foreignKeyModeChanged = (configuration.foreignKeyConstraintsEnabled
                != this.configuration.foreignKeyConstraintsEnabled)
        if (foreignKeyModeChanged) {
            // Foreign key constraints can only be changed if there are no transactions
            // in progress.  To make this clear, we throw an exception if there are
            // any acquired connections.
            if (!acquiredConnections.isEmpty()) {
                throw IllegalStateException(
                    "Foreign Key Constraints cannot "
                            + "be enabled or disabled while there are transactions in "
                            + "progress.  Finish all transactions and release all active "
                            + "database connections first."
                )
            }
        }

        if (this.configuration.openFlags != configuration.openFlags) {
            // If we are changing open flags and WAL mode at the same time, then
            // we have no choice but to close the primary connection beforehand
            // because there can only be one connection open when we change WAL mode.
            if (walModeChanged) {
                closeAvailableConnectionsAndLogExceptionsLocked()
            }

            // Try to reopen the primary connection using the new open flags then
            // close and discard all existing connections.
            // This might throw if the database is corrupt or cannot be opened in
            // the new mode in which case existing connections will remain untouched.
            val newPrimaryConnection = openConnectionLocked(
                configuration = configuration,
                primaryConnection = true,
            ) // might throw

            closeAvailableConnectionsAndLogExceptionsLocked()
            discardAcquiredConnectionsLocked()

            availablePrimaryConnection = newPrimaryConnection
            this.configuration.updateParametersFrom(configuration)
            setMaxConnectionPoolSizeLocked()
        } else {
            // Reconfigure the database connections in place.
            this.configuration.updateParametersFrom(configuration)
            setMaxConnectionPoolSizeLocked()

            closeExcessConnectionsAndLogExceptionsLocked()
            reconfigureAllConnectionsLocked()
        }
        wakeConnectionWaitersLocked()
    }

    /**
     * Acquires a connection from the pool.
     *
     * The caller must call [.releaseConnection] to release the connection
     * back to the pool when it is finished.  Failure to do so will result
     * in much unpleasantness.
     *
     * @param sql If not null, try to find a connection that already has
     * the specified SQL statement in its prepared statement cache.
     * @param connectionFlags The connection request flags.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The connection that was acquired, never null.
     *
     * @throws IllegalStateException if the pool has been closed.
     * @throws SQLiteException if a database error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun acquireConnection(
        sql: String?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?
    ): SQLiteConnection = waitForConnection(sql, connectionFlags, cancellationSignal)

    /**
     * Releases a connection back to the pool.
     *
     * It is ok to call this method after the pool has closed, to release
     * connections that were still in use at the time of closure.
     *
     * @param connection The connection to release.  Must not be null.
     *
     * @throws IllegalStateException if the connection was not acquired
     * from this pool or if it has already been released.
     */
    fun releaseConnection(connection: SQLiteConnection): Unit = synchronized(lock) {
        val status = acquiredConnections.remove(connection)
            ?: throw IllegalStateException(
                "Cannot perform this operation "
                        + "because the specified connection was not acquired "
                        + "from this pool or has already been released."
            )
        if (!isOpen) {
            closeConnectionAndLogExceptionsLocked(connection)
        } else if (connection.isPrimaryConnection) {
            if (recycleConnectionLocked(connection, status)) {
                assert(availablePrimaryConnection == null)
                availablePrimaryConnection = connection
            }
            wakeConnectionWaitersLocked()
        } else if (availableNonPrimaryConnections.size >= maxConnectionPoolSize - 1) {
            closeConnectionAndLogExceptionsLocked(connection)
        } else {
            if (recycleConnectionLocked(connection, status)) {
                availableNonPrimaryConnections.add(connection)
            }
            wakeConnectionWaitersLocked()
        }
    }

    // Can't throw.
    private fun recycleConnectionLocked(
        connection: SQLiteConnection,
        status: AcquiredConnectionStatus
    ): Boolean {
        var discard = false
        if (status == AcquiredConnectionStatus.RECONFIGURE) {
            try {
                connection.reconfigure(configuration) // might throw
            } catch (ex: RuntimeException) {
                Log.e(
                    TAG,
                    "Failed to reconfigure released connection, closing it: $connection",
                    ex
                )
                discard = true
            }
        }
        if (status == AcquiredConnectionStatus.DISCARD || discard) {
            closeConnectionAndLogExceptionsLocked(connection)
            return false
        }
        return true
    }

    /**
     * Returns true if the session should yield the connection due to
     * contention over available database connections.
     *
     * @param connection The connection owned by the session.
     * @param connectionFlags The connection request flags.
     * @return True if the session should yield its connection.
     *
     * @throws IllegalStateException if the connection was not acquired
     * from this pool or if it has already been released.
     */
    fun shouldYieldConnection(connection: SQLiteConnection, connectionFlags: Int): Boolean = synchronized(lock) {
        if (!acquiredConnections.containsKey(connection)) {
            throw IllegalStateException(
                "Cannot perform this operation "
                        + "because the specified connection was not acquired "
                        + "from this pool or has already been released."
            )
        }
        return if (isOpen) {
            isSessionBlockingImportantConnectionWaitersLocked(
                holdingPrimaryConnection = connection.isPrimaryConnection,
                connectionFlags = connectionFlags
            )
        } else {
            false
        }
    }

    /**
     * Collects statistics about database connection memory usage.
     *
     * @param dbStatsList The list to populate.
     */
    fun collectDbStats(dbStatsList: ArrayList<SQLiteDebug.DbStats>): Unit = synchronized(lock) {
        availablePrimaryConnection?.collectDbStats(dbStatsList)
        for (connection: SQLiteConnection in availableNonPrimaryConnections) {
            connection.collectDbStats(dbStatsList)
        }
        for (connection: SQLiteConnection in acquiredConnections.keys) {
            connection.collectDbStatsUnsafe(dbStatsList)
        }
    }

    // Might throw.
    private fun openConnectionLocked(
        configuration: SQLiteDatabaseConfiguration,
        primaryConnection: Boolean
    ): SQLiteConnection {
        val connectionId = nextConnectionId++
        return SQLiteConnection.open(this, configuration, connectionId, primaryConnection) // might throw
    }

    fun onConnectionLeaked() {
        // This code is running inside of the SQLiteConnection finalizer.
        //
        // We don't know whether it is just the connection that has been finalized (and leaked)
        // or whether the connection pool has also been or is about to be finalized.
        // Consequently, it would be a bad idea to try to grab any locks or to
        // do any significant work here.  So we do the simplest possible thing and
        // set a flag.  waitForConnection() periodically checks this flag (when it
        // times out) so that it can recover from leaked connections and wake
        // itself or other threads up if necessary.
        //
        // You might still wonder why we don't try to do more to wake up the waiters
        // immediately.  First, as explained above, it would be hard to do safely
        // unless we started an extra Thread to function as a reference queue.  Second,
        // this is never supposed to happen in normal operation.  Third, there is no
        // guarantee that the GC will actually detect the leak in a timely manner so
        // it's not all that important that we recover from the leak in a timely manner
        // either.  Fourth, if a badly behaved application finds itself hung waiting for
        // several seconds while waiting for a leaked connection to be detected and recreated,
        // then perhaps its authors will have added incentive to fix the problem!

        Log.w(
            TAG, "A SQLiteConnection object for database '"
                    + configuration.label + "' was leaked!  Please fix your application "
                    + "to end transactions in progress properly and to close the database "
                    + "when it is no longer needed."
        )

        connectionLeaked.set(true)
    }

    // Can't throw.
    private fun closeAvailableConnectionsAndLogExceptionsLocked() {
        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked()
        availablePrimaryConnection?.let {
            closeConnectionAndLogExceptionsLocked(it)
            availablePrimaryConnection = null
        }
    }

    // Can't throw.
    private fun closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked() {
        for (connection in availableNonPrimaryConnections) {
            closeConnectionAndLogExceptionsLocked(connection)
        }
        availableNonPrimaryConnections.clear()
    }

    // Can't throw.
    private fun closeExcessConnectionsAndLogExceptionsLocked() {
        var availableCount = availableNonPrimaryConnections.size
        while (availableCount-- > maxConnectionPoolSize - 1) {
            val connection = availableNonPrimaryConnections.removeAt(availableCount)
            closeConnectionAndLogExceptionsLocked(connection)
        }
    }

    // Can't throw.
    private fun closeConnectionAndLogExceptionsLocked(connection: SQLiteConnection) {
        try {
            connection.close() // might throw
        } catch (ex: RuntimeException) {
            Log.e(
                TAG, "Failed to close connection, its fate is now in the hands "
                        + "of the merciful GC: " + connection, ex
            )
        }
    }

    // Can't throw.
    private fun discardAcquiredConnectionsLocked() {
        markAcquiredConnectionsLocked(AcquiredConnectionStatus.DISCARD)
    }

    // Can't throw.
    private fun reconfigureAllConnectionsLocked() {
        availablePrimaryConnection?.let { connection ->
            try {
                connection.reconfigure(configuration) // might throw
            } catch (ex: RuntimeException) {
                Log.e(
                    TAG,
                    "Failed to reconfigure available primary connection, closing it: $connection",
                    ex
                )
                closeConnectionAndLogExceptionsLocked(connection)
                availablePrimaryConnection = null
            }
        }

        var count = availableNonPrimaryConnections.size
        var i = 0
        while (i < count) {
            val connection = availableNonPrimaryConnections[i]
            try {
                connection.reconfigure(configuration) // might throw
            } catch (ex: RuntimeException) {
                Log.e(
                    TAG,
                    "Failed to reconfigure available non-primary connection, closing it: $connection",
                    ex
                )
                closeConnectionAndLogExceptionsLocked(connection)
                availableNonPrimaryConnections.removeAt(i--)
                count -= 1
            }
            i++
        }

        markAcquiredConnectionsLocked(AcquiredConnectionStatus.RECONFIGURE)
    }

    // Can't throw.
    private fun markAcquiredConnectionsLocked(status: AcquiredConnectionStatus) {
        val keysToUpdate = acquiredConnections.mapNotNull { (key, oldStatus) ->
            if (status != oldStatus && oldStatus != AcquiredConnectionStatus.DISCARD) {
                key
            } else {
                null
            }
        }
        keysToUpdate.forEach { key -> acquiredConnections[key] = status }
    }

    // Might throw.
    private fun waitForConnection(
        sql: String?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?
    ): SQLiteConnection {
        val wantPrimaryConnection = (connectionFlags and CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY) != 0

        val waiter: ConnectionWaiter
        val nonce: Int
        synchronized(lock) {
            throwIfClosedLocked()
            // Abort if canceled.
            cancellationSignal?.throwIfCanceled()

            // Try to acquire a connection.
            val connection: SQLiteConnection? = if (!wantPrimaryConnection) {
                tryAcquireNonPrimaryConnectionLocked(sql, connectionFlags) // might throw
            } else {
                null
            } ?: tryAcquirePrimaryConnectionLocked(connectionFlags) // might throw
            if (connection != null) {
                return connection
            }

            // No connections available.  Enqueue a waiter in priority order.
            val priority = getPriority(connectionFlags)
            val startTime = SystemClock.uptimeMillis()
            waiter = obtainConnectionWaiterLocked(
                thread = Thread.currentThread(),
                startTime = startTime,
                priority = priority,
                wantPrimaryConnection = wantPrimaryConnection,
                sql = sql,
                connectionFlags = connectionFlags
            )
            var predecessor: ConnectionWaiter? = null
            var successor = connectionWaiterQueue
            while (successor != null) {
                if (priority > successor.priority) {
                    waiter.next = successor
                    break
                }
                predecessor = successor
                successor = successor.next
            }
            if (predecessor != null) {
                predecessor.next = waiter
            } else {
                connectionWaiterQueue = waiter
            }
            nonce = waiter.nonce
        }

        // Set up the cancellation listener.
        cancellationSignal?.setOnCancelListener {
            synchronized(lock) {
                if (waiter.nonce == nonce) {
                    cancelConnectionWaiterLocked(waiter)
                }
            }
        }
        try {
            // Park the thread until a connection is assigned or the pool is closed.
            // Rethrow an exception from the wait, if we got one.
            var busyTimeoutMillis = CONNECTION_POOL_BUSY_MILLIS
            var nextBusyTimeoutTime = waiter.startTime + busyTimeoutMillis
            while (true) {
                // Detect and recover from connection leaks.
                if (connectionLeaked.compareAndSet(true, false)) {
                    synchronized(lock) {
                        wakeConnectionWaitersLocked()
                    }
                }

                // Wait to be unparked (may already have happened), a timeout, or interruption.
                LockSupport.parkNanos(this, busyTimeoutMillis * 1000000L)

                // Clear the interrupted flag, just in case.
                Thread.interrupted()

                // Check whether we are done waiting yet.
                synchronized(lock) {
                    throwIfClosedLocked()
                    val connection = waiter.assignedConnection
                    val ex = waiter.exception
                    if (connection != null || ex != null) {
                        recycleConnectionWaiterLocked(waiter)
                        if (connection != null) {
                            return connection
                        }
                        throw (ex)!! // rethrow!
                    }

                    val now = SystemClock.uptimeMillis()
                    if (now < nextBusyTimeoutTime) {
                        busyTimeoutMillis = now - nextBusyTimeoutTime
                    } else {
                        logConnectionPoolBusyLocked(now - waiter.startTime, connectionFlags)
                        busyTimeoutMillis = CONNECTION_POOL_BUSY_MILLIS
                        nextBusyTimeoutTime = now + busyTimeoutMillis
                    }
                }
            }
        } finally {
            // Remove the cancellation listener.
            cancellationSignal?.setOnCancelListener(null)
        }
    }

    // Can't throw.
    private fun cancelConnectionWaiterLocked(waiter: ConnectionWaiter) {
        if (waiter.assignedConnection != null || waiter.exception != null) {
            // Waiter is done waiting but has not woken up yet.
            return
        }

        // Waiter must still be waiting.  Dequeue it.
        var predecessor: ConnectionWaiter? = null
        var current = connectionWaiterQueue
        while (current != waiter) {
            checkNotNull(current)
            predecessor = current
            current = current.next
        }
        if (predecessor != null) {
            predecessor.next = waiter.next
        } else {
            connectionWaiterQueue = waiter.next
        }

        // Send the waiter an exception and unpark it.
        waiter.exception = OperationCanceledException()
        LockSupport.unpark(waiter.thread)

        // Check whether removing this waiter will enable other waiters to make progress.
        wakeConnectionWaitersLocked()
    }

    // Can't throw.
    private fun logConnectionPoolBusyLocked(waitMillis: Long, connectionFlags: Int) {
        val thread = Thread.currentThread()
        val msg = StringBuilder()
        msg.append("The connection pool for database '").append(configuration.label)
        msg.append("' has been unable to grant a connection to thread ")
        msg.append(thread.id).append(" (").append(thread.name).append(") ")
        msg.append("with flags 0x").append(Integer.toHexString(connectionFlags))
        msg.append(" for ").append(waitMillis * 0.001f).append(" seconds.\n")

        val requests = ArrayList<String>()
        var activeConnections = 0
        var idleConnections = 0
        if (!acquiredConnections.isEmpty()) {
            for (connection in acquiredConnections.keys) {
                val description = connection!!.describeCurrentOperationUnsafe()
                if (description != null) {
                    requests.add(description)
                    activeConnections += 1
                } else {
                    idleConnections += 1
                }
            }
        }
        var availableConnections = availableNonPrimaryConnections.size
        if (availablePrimaryConnection != null) {
            availableConnections += 1
        }

        msg.append("Connections: ").append(activeConnections).append(" active, ")
        msg.append(idleConnections).append(" idle, ")
        msg.append(availableConnections).append(" available.\n")

        if (requests.isNotEmpty()) {
            msg.append("\nRequests in progress:\n")
            for (request in requests) {
                msg.append("  ").append(request).append("\n")
            }
        }

        Log.w(TAG, msg.toString())
    }

    // Can't throw.
    private fun wakeConnectionWaitersLocked() {
        // Unpark all waiters that have requests that we can fulfill.
        // This method is designed to not throw runtime exceptions, although we might send
        // a waiter an exception for it to rethrow.
        var predecessor: ConnectionWaiter? = null
        var waiter = connectionWaiterQueue
        var primaryConnectionNotAvailable = false
        var nonPrimaryConnectionNotAvailable = false
        while (waiter != null) {
            var unpark = false
            if (!isOpen) {
                unpark = true
            } else {
                try {
                    var connection: SQLiteConnection? = null
                    if (!waiter.wantPrimaryConnection && !nonPrimaryConnectionNotAvailable) {
                        connection = tryAcquireNonPrimaryConnectionLocked(
                            waiter.sql, waiter.connectionFlags
                        ) // might throw
                        if (connection == null) {
                            nonPrimaryConnectionNotAvailable = true
                        }
                    }
                    if (connection == null && !primaryConnectionNotAvailable) {
                        connection = tryAcquirePrimaryConnectionLocked(
                            waiter.connectionFlags
                        ) // might throw
                        if (connection == null) {
                            primaryConnectionNotAvailable = true
                        }
                    }
                    if (connection != null) {
                        waiter.assignedConnection = connection
                        unpark = true
                    } else if (nonPrimaryConnectionNotAvailable && primaryConnectionNotAvailable) {
                        // There are no connections available and the pool is still open.
                        // We cannot fulfill any more connection requests, so stop here.
                        break
                    }
                } catch (ex: RuntimeException) {
                    // Let the waiter handle the exception from acquiring a connection.
                    waiter.exception = ex
                    unpark = true
                }
            }

            val successor = waiter.next
            if (unpark) {
                if (predecessor != null) {
                    predecessor.next = successor
                } else {
                    connectionWaiterQueue = successor
                }
                waiter.next = null

                LockSupport.unpark(waiter.thread)
            } else {
                predecessor = waiter
            }
            waiter = successor
        }
    }

    // Might throw.
    private fun tryAcquirePrimaryConnectionLocked(connectionFlags: Int): SQLiteConnection? {
        // If the primary connection is available, acquire it now.
        var connection = availablePrimaryConnection
        if (connection != null) {
            availablePrimaryConnection = null
            finishAcquireConnectionLocked(connection, connectionFlags) // might throw
            return connection
        }

        // Make sure that the primary connection actually exists and has just been acquired.
        for (acquiredConnection in acquiredConnections.keys) {
            if (acquiredConnection.isPrimaryConnection) {
                return null
            }
        }

        // Uhoh.  No primary connection!  Either this is the first time we asked
        // for it, or maybe it leaked?
        connection = openConnectionLocked(
            configuration = configuration,
            primaryConnection = true
        ) // might throw
        finishAcquireConnectionLocked(connection, connectionFlags) // might throw
        return connection
    }

    // Might throw.
    private fun tryAcquireNonPrimaryConnectionLocked(
        sql: String?,
        connectionFlags: Int
    ): SQLiteConnection? {
        // Try to acquire the next connection in the queue.
        var connection: SQLiteConnection
        val availableCount = availableNonPrimaryConnections.size
        if (availableCount > 1 && sql != null) {
            // If we have a choice, then prefer a connection that has the
            // prepared statement in its cache.
            for (i in 0 until availableCount) {
                connection = availableNonPrimaryConnections[i]
                if (connection.isPreparedStatementInCache(sql)) {
                    availableNonPrimaryConnections.removeAt(i)
                    finishAcquireConnectionLocked(connection, connectionFlags) // might throw
                    return connection
                }
            }
        }
        if (availableCount > 0) {
            // Otherwise, just grab the next one.
            connection = availableNonPrimaryConnections.removeAt(availableCount - 1)
            finishAcquireConnectionLocked(connection, connectionFlags) // might throw
            return connection
        }

        // Expand the pool if needed.
        var openConnections = acquiredConnections.size
        if (availablePrimaryConnection != null) {
            openConnections += 1
        }
        if (openConnections >= maxConnectionPoolSize) {
            return null
        }
        connection = openConnectionLocked(
            configuration = configuration,
            primaryConnection = false,
        ) // might throw
        finishAcquireConnectionLocked(connection, connectionFlags) // might throw
        return connection
    }

    // Might throw.
    private fun finishAcquireConnectionLocked(connection: SQLiteConnection, connectionFlags: Int) {
        try {
            val readOnly = (connectionFlags and CONNECTION_FLAG_READ_ONLY) != 0
            connection.setOnlyAllowReadOnlyOperations(readOnly)

            acquiredConnections[connection] = AcquiredConnectionStatus.NORMAL
        } catch (ex: RuntimeException) {
            Log.e(
                TAG,
                "Failed to prepare acquired connection for session, closing it: $connection, connectionFlags=$connectionFlags"
            )
            closeConnectionAndLogExceptionsLocked(connection)
            throw ex // rethrow!
        }
    }

    private fun isSessionBlockingImportantConnectionWaitersLocked(
        holdingPrimaryConnection: Boolean,
        connectionFlags: Int
    ): Boolean {
        var waiter = connectionWaiterQueue
        if (waiter != null) {
            val priority = getPriority(connectionFlags)
            do {
                // Only worry about blocked connections that have same or lower priority.
                if (priority > waiter!!.priority) {
                    break
                }

                // If we are holding the primary connection then we are blocking the waiter.
                // Likewise, if we are holding a non-primary connection and the waiter
                // would accept a non-primary connection, then we are blocking the waier.
                if (holdingPrimaryConnection || !waiter.wantPrimaryConnection) {
                    return true
                }

                waiter = waiter.next
            } while (waiter != null)
        }
        return false
    }

    private fun setMaxConnectionPoolSizeLocked() {
        maxConnectionPoolSize = if (!SQLiteDatabase.hasCodec()
            && (configuration.openFlags and SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING) != 0
        ) {
            SQLiteGlobal.wALConnectionPoolSize
        } else {
            // TODO: We don't actually need to restrict the connection pool size to 1
            // for non-WAL databases.  There might be reasons to use connection pooling
            // with other journal modes.  For now, enabling connection pooling and
            // using WAL are the same thing in the API.
            1
        }
    }

    private fun throwIfClosedLocked() = check(isOpen) {
        "Cannot perform this operation because the connection pool has been closed."
    }

    private fun obtainConnectionWaiterLocked(
        thread: Thread,
        startTime: Long,
        priority: Int,
        wantPrimaryConnection: Boolean,
        sql: String?,
        connectionFlags: Int
    ): ConnectionWaiter {
        var waiter = connectionWaiterPool
        if (waiter != null) {
            connectionWaiterPool = waiter.next
            waiter.next = null
        } else {
            waiter = ConnectionWaiter()
        }
        waiter.thread = thread
        waiter.startTime = startTime
        waiter.priority = priority
        waiter.wantPrimaryConnection = wantPrimaryConnection
        waiter.sql = sql
        waiter.connectionFlags = connectionFlags
        return waiter
    }

    private fun recycleConnectionWaiterLocked(waiter: ConnectionWaiter) {
        waiter.next = connectionWaiterPool
        waiter.thread = null
        waiter.sql = null
        waiter.assignedConnection = null
        waiter.exception = null
        waiter.nonce += 1
        connectionWaiterPool = waiter
    }

    fun enableLocalizedCollators() = synchronized(lock) {
        if (!acquiredConnections.isEmpty() || availablePrimaryConnection == null) {
            throw IllegalStateException("Cannot enable localized collators while database is in use")
        }
        availablePrimaryConnection!!.enableLocalizedCollators()
    }

    /**
     * Dumps debugging information about this connection pool.
     *
     * @param printer The printer to receive the dump, not null.
     * @param verbose True to dump more verbose information.
     */
    fun dump(printer: Printer, verbose: Boolean): Unit = synchronized(lock) {
        printer.println("Connection pool for " + configuration.path + ":")
        printer.println("  Open: $isOpen")
        printer.println("  Max connections: $maxConnectionPoolSize")

        printer.println("  Available primary connection:")
        if (availablePrimaryConnection != null) {
            availablePrimaryConnection!!.dump(printer, verbose)
        } else {
            printer.println("<none>")
        }

        printer.println("  Available non-primary connections:")
        if (availableNonPrimaryConnections.isNotEmpty()) {
            for (connection: SQLiteConnection in availableNonPrimaryConnections) {
                connection.dump(printer, verbose)
            }
        } else {
            printer.println("<none>")
        }

        printer.println("  Acquired connections:")
        if (!acquiredConnections.isEmpty()) {
            for (entry in acquiredConnections.entries) {
                val connection = entry.key
                connection!!.dumpUnsafe(printer, verbose)
                printer.println("  Status: " + entry.value)
            }
        } else {
            printer.println("<none>")
        }

        printer.println("  Connection waiters:")
        if (connectionWaiterQueue != null) {
            var i = 0
            val now = SystemClock.uptimeMillis()
            var waiter = connectionWaiterQueue
            while (waiter != null
            ) {
                printer.println(
                    "$i: waited for ${(now - waiter.startTime) * 0.001f} ms - thread=${waiter.thread}, " +
                            "priority=${waiter.priority}, " +
                            "sql='${waiter.sql}'"
                )
                waiter = waiter.next
                i++
            }
        } else {
            printer.println("<none>")
        }
    }

    override fun toString(): String = "SQLiteConnectionPool: ${configuration.path}"

    private class ConnectionWaiter {
        var next: ConnectionWaiter? = null
        var thread: Thread? = null
        var startTime: Long = 0
        var priority: Int = 0
        var wantPrimaryConnection: Boolean = false
        var sql: String? = null
        var connectionFlags: Int = 0
        var assignedConnection: SQLiteConnection? = null
        var exception: RuntimeException? = null
        var nonce: Int = 0
    }

    companion object {
        private const val TAG = "SQLiteConnectionPool"

        // Amount of time to wait in milliseconds before unblocking acquireConnection
        // and logging a message about the connection pool being busy.
        const val CONNECTION_POOL_BUSY_MILLIS = (30 * 1000).toLong() // 30 seconds

        /**
         * Connection flag: Read-only.
         *
         *
         * This flag indicates that the connection will only be used to
         * perform read-only operations.
         *
         */
        const val CONNECTION_FLAG_READ_ONLY: Int = 1

        /**
         * Connection flag: Primary connection affinity.
         *
         *
         * This flag indicates that the primary connection is required.
         * This flag helps support legacy applications that expect most data modifying
         * operations to be serialized by locking the primary database connection.
         * Setting this flag essentially implements the old "db lock" concept by preventing
         * an operation from being performed until it can obtain exclusive access to
         * the primary connection.
         *
         */
        const val CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY: Int = 1 shl 1

        /**
         * Connection flag: Connection is being used interactively.
         *
         *
         * This flag indicates that the connection is needed by the UI thread.
         * The connection pool can use this flag to elevate the priority
         * of the database connection request.
         *
         */
        const val CONNECTION_FLAG_INTERACTIVE: Int = 1 shl 2

        /**
         * Opens a connection pool for the specified database.
         *
         * @param configuration The database configuration.
         * @return The connection pool.
         *
         * @throws SQLiteException if a database error occurs.
         */
        fun open(configuration: SQLiteDatabaseConfiguration): SQLiteConnectionPool = SQLiteConnectionPool(configuration)
            .apply(SQLiteConnectionPool::open)

        private fun getPriority(connectionFlags: Int): Int {
            return if ((connectionFlags and CONNECTION_FLAG_INTERACTIVE) != 0) 1 else 0
        }
    }
}