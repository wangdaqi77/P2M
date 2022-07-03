package com.p2m.core.event

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * [BackgroundLiveEvent] subclass which may observe other [BackgroundLiveEvent] objects and react on
 * `OnChanged` events from them.
 *
 * This class correctly propagates its active/inactive states down to source [BackgroundLiveEvent]
 * objects.
 *
 * Consider the following scenario: we have 2 instances of [BackgroundLiveEvent], let's name them
 * `liveEvent1` and `liveEvent2`, and we want to merge their emissions in one object:
 * `liveEventMerger`. Then, `liveEvent1` and `liveEvent2` will become sources for
 * the `MediatorBackgroundLiveEvent liveEventMerger` and every time `onChanged` callback
 * is called for either of them, we set a new value in `liveEventMerger`.
 *
 * ```kotlin
 * val liveEvent1: BackgroundLiveEvent<Int> = ...
 * val liveEvent2: BackgroundLiveEvent<Int> = ...
 *
 * val liveEventMerger: MediatorBackgroundLiveEvent<Int> = MediatorBackgroundLiveEvent()
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
 * })
 * ```
 *
 * @param T The type of data hold by this instance
 */
internal class MediatorBackgroundLiveEvent<T>() : wang.lifecycle.MediatorBackgroundLiveEvent<T>(), BackgroundLiveEvent<T> {

    /**
     * Starts to listen the given `source` BackgroundLiveEvent, `onChanged` observer will be called
     * when `source` value was changed.
     *
     *
     * `onChanged` callback will be called only when this `MediatorBackgroundLiveEvent` is active.
     *
     *  If the given BackgroundLiveEvent is already added as a source but with a different Observer,
     * [IllegalArgumentException] will be thrown.
     *
     * @param source    the `BackgroundLiveEvent` to listen to
     * @param onChanged The observer that will receive the events
     * @param S         The type of data hold by `source` BackgroundLiveEvent
     */
    @Suppress("UNCHECKED_CAST")
    fun <S> addSource(source: BackgroundLiveEvent<S>, onChanged: BackgroundObserver<in S>) {
        super.addSource(source as wang.lifecycle.BackgroundLiveEvent<S>, onChanged)
    }

    /**
     * Stops to listen the given `BackgroundLiveEvent`.
     *
     * @param toRemote `BackgroundLiveEvent` to stop to listen
     * @param S        the type of data hold by `source` BackgroundLiveEvent
     */
    @Suppress("UNCHECKED_CAST")
    fun <S> removeSource(toRemote: BackgroundLiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.BackgroundLiveEvent<S>,)
    }

    @MainThread
    @Suppress("UNCHECKED_CAST")
    fun <S> addSource(source: LiveEvent<S>, onChanged: Observer<in S>) {
        super.addSource(source as wang.lifecycle.LiveEvent<S>, onChanged)
    }

    @MainThread
    @Suppress("UNCHECKED_CAST")
    fun <S> removeSource(toRemote: LiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.LiveEvent<S>)
    }

    @MainThread
    fun <S> addLiveDataSource(source: LiveData<S>, onChanged: Observer<in S>) {
        super.addSource(source, onChanged)
    }

    @MainThread
    fun <S> removeLiveDataSource(toRemote: LiveData<S>) {
        super.removeSource(toRemote)
    }

    override fun observe(owner: LifecycleOwner, observer: BackgroundObserver<in T>) {
        super.observe(owner, observer)
    }

    override fun observeForever(observer: BackgroundObserver<in T>) {
        super.observeForever(observer)
    }

    override fun observeNoSticky(owner: LifecycleOwner, observer: BackgroundObserver<in T>) {
        super.observeNoSticky(owner, observer)
    }

    override fun observeForeverNoSticky(observer: BackgroundObserver<in T>) {
        super.observeForeverNoSticky(observer)
    }

    override fun observeNoLoss(owner: LifecycleOwner, observer: BackgroundObserver<in T>) {
        super.observeNoLoss(owner, observer)
    }

    override fun observeForeverNoLoss(observer: BackgroundObserver<in T>) {
        super.observeForeverNoLoss(observer)
    }

    override fun observeNoStickyNoLoss(owner: LifecycleOwner, observer: BackgroundObserver<in T>) {
        super.observeNoStickyNoLoss(owner, observer)
    }

    override fun observeForeverNoStickyNoLoss(observer: BackgroundObserver<in T>) {
        super.observeForeverNoStickyNoLoss(observer)
    }

    override fun removeObserver(observer: BackgroundObserver<in T>) {
        super.removeObserver(observer)
    }

}