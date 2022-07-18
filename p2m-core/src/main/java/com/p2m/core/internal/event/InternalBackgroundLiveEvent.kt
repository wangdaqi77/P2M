package com.p2m.core.internal.event

import androidx.lifecycle.LifecycleOwner
import com.p2m.core.event.BackgroundObserver
import wang.lifecycle.MutableBackgroundLiveEvent


/**
 * [InternalBackgroundLiveEvent] publicly exposes all observe method.
 *
 * @param T The type of data hold by this instance
 */
internal open class InternalBackgroundLiveEvent<T> : MutableBackgroundLiveEvent<T>, com.p2m.core.event.MutableBackgroundLiveEvent<T> {

    /**
     * Creates a InternalBackgroundLiveEvent initialized with the given value.
     *
     * @property value initial value
     */
    constructor(value: T) : super(value)

    /**
     * Creates a InternalBackgroundLiveEvent with no value assigned to it.
     */
    constructor() : super()

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