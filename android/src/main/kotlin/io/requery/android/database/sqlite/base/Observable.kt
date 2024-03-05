package io.requery.android.database.sqlite.base

import android.database.ContentObserver
import android.database.DataSetObserver
import android.net.Uri

// Copy of the android.database.Observable
internal open class Observable<T: Any> {
    protected val observers: MutableList<T> = mutableListOf()

    open fun registerObserver(observer: T) {
        synchronized(observers) {
            check(!observers.contains(observer)) { "Observer $observer is already registered." }
            observers.add(observer)
        }
    }

    fun unregisterObserver(observer: T) {
        synchronized(observers) {
            check(observers.contains(observer)) { "Observer $observer was not registered." }
            observers.remove(observer)
        }
    }

    fun unregisterAll() {
        synchronized(observers) {
            observers.clear()
        }
    }
}

internal class ContentObservable : Observable<ContentObserver>() {
    fun dispatchChange(selfChange: Boolean, uri: Uri?) {
        synchronized(observers) {
            for (observer in observers) {
                if (!selfChange || observer.deliverSelfNotifications()) {
                    observer.dispatchChange(selfChange, uri)
                }
            }
        }
    }
}

internal class DataSetObservable : Observable<DataSetObserver>() {
    fun notifyChanged() {
        synchronized(observers) {
            for (i in observers.indices.reversed()) {
                observers[i].onChanged()
            }
        }
    }

    /**
     * Invokes [DataSetObserver.onInvalidated] on each observer.
     * Called when the data set is no longer valid and cannot be queried again,
     * such as when the data set has been closed.
     */
    fun notifyInvalidated() {
        synchronized(observers) {
            for (i in observers.indices.reversed()) {
                observers[i].onInvalidated()
            }
        }
    }
}