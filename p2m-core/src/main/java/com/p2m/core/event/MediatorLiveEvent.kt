package com.p2m.core.event

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * [LiveEvent] subclass which may observe other `LiveEvent` objects and react on
 * `OnChanged` events from them.
 *
 * This class correctly propagates its active/inactive states down to source [LiveEvent]
 * objects.
 *
 * Consider the following scenario: we have 2 instances of [LiveEvent], let's name them
 * `liveEvent1` and `liveEvent2`, and we want to merge their emissions in one object:
 * `liveEventMerger`. Then, `liveEvent1` and `liveEvent2` will become sources for
 * the `MediatorLiveEvent liveEventMerger` and every time `onChanged` callback
 * is called for either of them, we set a new value in `liveEventMerger`.
 *
 * ```kotlin
 * val liveEvent1: LiveEvent<Int> = ...
 * val liveEvent2: LiveEvent<Int> = ...
 *
 * val liveEventMerger: MediatorLiveEvent<Int> = MediatorLiveEvent()
 * liveEventMerger.addSource(liveEvent1, Observer { value -> liveEventMerger.setValue(value) })
 * liveEventMerger.addSource(liveEvent2, Observer { value -> liveEventMerger.setValue(value) })
 * ```
 *
 * Let's consider that we only want 10 values emitted by `liveEvent1`, to be
 * merged in the `liveEventMerger`. Then, after 10 values, we can stop listening to `liveEvent1`
 * and remove it as a source.
 * ```kotlin
 * liveEventMerger.addSource(liveEvent1, object : Observer<Int> {
 *      private val count = 1
 *
 *      public override fun onChanged(s: Int) {
 *          count++
 *          liveEventMerger.setValue(s)
 *          if (count > 10) {
 *              liveEventMerger.removeSource(liveEvent1)
 *          }
 *      }
 * });
 * ```
 *
 * @param T The type of data hold by this instance
 */
class MediatorLiveEvent<T> : wang.lifecycle.MediatorLiveEvent<T>(), LiveEvent<T> {

    /**
     * Starts to listen the given `source` LiveEvent, `onChanged` observer will be called
     * when `source` value was changed.
     *
     *
     * `onChanged` callback will be called only when this `MediatorLiveEvent` is active.
     *
     *  If the given LiveEvent is already added as a source but with a different Observer,
     * [IllegalArgumentException] will be thrown.
     *
     * @param source    the `LiveEvent` to listen to
     * @param onChanged The observer that will receive the events
     * @param S         The type of data hold by `source` LiveEvent
     */
    @MainThread
    @Suppress("UNCHECKED_CAST")
    fun <S> addSource(source: LiveEvent<S>, onChanged: Observer<in S>) {
        super.addSource(source as wang.lifecycle.LiveEvent<S>, onChanged)
    }

    /**
     * Stops to listen the given `LiveEvent`.
     *
     * @param toRemote `LiveEvent` to stop to listen
     * @param S        the type of data hold by `source` LiveEvent
     */
    @MainThread
    @Suppress("UNCHECKED_CAST")
    fun <S> removeSource(toRemote: LiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.LiveEvent<S>)
    }

    @Suppress("UNCHECKED_CAST")
    fun <S> addSource(source: BackgroundLiveEvent<S>, onChanged: BackgroundObserver<in S>) {
        super.addSource(source as wang.lifecycle.BackgroundLiveEvent<S>, onChanged)
    }

    @Suppress("UNCHECKED_CAST")
    fun <S> removeSource(toRemote: BackgroundLiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.BackgroundLiveEvent<S>,)
    }

    @MainThread
    fun <S> addLiveDataSource(source: LiveData<S>, onChanged: Observer<in S>) {
        super.addSource(source, onChanged)
    }

    @MainThread
    fun <S> removeLiveDataSource(toRemote: LiveData<S>) {
        super.removeSource(toRemote)
    }
}
