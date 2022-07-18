package com.p2m.core.internal.event

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.p2m.core.event.*

internal class InternalMediatorBackgroundLiveEvent<T>() : wang.lifecycle.MediatorBackgroundLiveEvent<T>(),
    MediatorBackgroundLiveEvent<T>,
    MediatorEvent {

    @Suppress("UNCHECKED_CAST")
    override fun <S> addSource(source: BackgroundLiveEvent<S>, onChanged: BackgroundObserver<in S>) {
        super.addSource(source as wang.lifecycle.BackgroundLiveEvent<S>, onChanged)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S> removeSource(toRemote: BackgroundLiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.BackgroundLiveEvent<S>,)
    }

    @MainThread
    @Suppress("UNCHECKED_CAST")
    override fun <S> addSource(source: LiveEvent<S>, onChanged: Observer<in S>) {
        super.addSource(source as wang.lifecycle.LiveEvent<S>, onChanged)
    }

    @MainThread
    @Suppress("UNCHECKED_CAST")
    override fun <S> removeSource(toRemote: LiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.LiveEvent<S>)
    }

    @MainThread
    override fun <S> addLiveDataSource(source: LiveData<S>, onChanged: Observer<in S>) {
        super.addSource(source, onChanged)
    }

    @MainThread
    override fun <S> removeLiveDataSource(toRemote: LiveData<S>) {
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