package com.p2m.core.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.p2m.annotation.module.api.*
import com.p2m.core.internal.event.InternalBackgroundLiveEvent
import kotlin.reflect.KProperty

/**
 * Defined a common interface for background event holder class.
 *
 * See [live-event library](https://github.com/wangdaqi77/live-event)
 *
 * @see EventOn.BACKGROUND - gen by KAPT.
 * @see BackgroundObserver - can specified thread to receive event.
 */
interface BackgroundLiveEvent<T> {

    class Delegate<T> {
        private val real by lazy { InternalBackgroundLiveEvent<T>() }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): BackgroundLiveEvent<T> = real
    }

    class MutableDelegate<T> {
        private val real by lazy { InternalBackgroundLiveEvent<T>() }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableBackgroundLiveEvent<T> = real
    }

    class InternalMutableDelegate<T>(private val real: BackgroundLiveEvent<T>) {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableBackgroundLiveEvent<T> = real as MutableBackgroundLiveEvent
    }

    fun observe(owner: LifecycleOwner, observer: BackgroundObserver<in T>)

    fun observeForever(observer: BackgroundObserver<in T>)

    fun observeNoSticky(owner: LifecycleOwner, observer: BackgroundObserver<in T>)

    fun observeForeverNoSticky(observer: BackgroundObserver<in T>)

    fun observeNoLoss(owner: LifecycleOwner, observer: BackgroundObserver<in T>)

    fun observeForeverNoLoss(observer: BackgroundObserver<in T>)

    fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: BackgroundObserver<in T>)

    fun observeForeverNoStickyNoLoss(observer: BackgroundObserver<in T>)

    fun removeObservers(owner: LifecycleOwner)

    fun removeObserver(observer: BackgroundObserver<in T>)

    fun hasActiveObservers(): Boolean

    fun hasObservers(): Boolean

    fun getValue(): T?
}

interface MutableBackgroundLiveEvent<T> : BackgroundLiveEvent<T> {

    fun postValue(value: T)

    fun setValue(value: T)
}

enum class EventDispatcher {
    DEFAULT,        // Receiving event in default thread, not recommended time-consuming work.
    BACKGROUND,     // Receiving event in background thread, not recommended time-consuming work.
    ASYNC,          // Receiving event in thread pool.
    MAIN            // Receiving event in main thread, not recommended time-consuming work.
}


class BackgroundObserver<T>(eventDispatcher: EventDispatcher, observer: Observer<T>) :
    wang.lifecycle.BackgroundObserver<T>(
        dispatcher = when (eventDispatcher) {
            EventDispatcher.DEFAULT -> wang.lifecycle.EventDispatcher.DEFAULT
            EventDispatcher.BACKGROUND -> wang.lifecycle.EventDispatcher.BACKGROUND
            EventDispatcher.ASYNC -> wang.lifecycle.EventDispatcher.ASYNC
            EventDispatcher.MAIN -> wang.lifecycle.EventDispatcher.MAIN
        },
        observer = observer
    )

/**
 * A simple callback that can specified thread to receive event from [BackgroundLiveEvent].
 */
fun <T> BackgroundObserver(eventDispatcher: EventDispatcher = EventDispatcher.DEFAULT, onChanged: (T) -> Unit): BackgroundObserver<T> {
    return BackgroundObserver(eventDispatcher, Observer { onChanged(it) })
}