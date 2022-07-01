package com.p2m.core.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.p2m.core.internal.event.InternalLiveEvent
import com.p2m.annotation.module.api.*

/**
 * Defined a common interface for event holder class.
 *
 * See [live-event library](https://github.com/wangdaqi77/live-event)
 *
 * @see EventOn.MAIN - gen by KAPT.
 */
interface LiveEvent<T>{
    companion object {
        fun <T> delegate(): Lazy<LiveEvent<T>> =
            lazy { InternalLiveEvent() }

        fun <T> delegateMutable(): Lazy<MutableLiveEvent<T>> =
            lazy { InternalLiveEvent() }

        fun <T> toMutable(real: LiveEvent<T>): Lazy<MutableLiveEvent<T>> =
            lazy(LazyThreadSafetyMode.NONE) { real as MutableLiveEvent }
    }

    fun observe(owner: LifecycleOwner, observer: Observer<in T>)

    fun observeForever(observer: Observer<in T>)

    fun observeNoSticky(owner: LifecycleOwner, observer: Observer<in T>)

    fun observeForeverNoSticky(observer: Observer<in T>)

    fun observeNoLoss(owner: LifecycleOwner, observer: Observer<in T>)

    fun observeForeverNoLoss(observer: Observer<in T>)

    fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: Observer<in T>)

    fun observeForeverNoStickyNoLoss(observer: Observer<in T>)

    fun removeObservers(owner: LifecycleOwner)

    fun removeObserver(observer: Observer<in T>)

    fun hasActiveObservers(): Boolean

    fun hasObservers(): Boolean

    fun getValue(): T?

    fun asLiveData(): LiveData<T>
}

interface MutableLiveEvent<T> : LiveEvent<T> {

    fun postValue(value: T)

    fun setValue(value: T)
}
