package com.p2m.core.internal.event

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.p2m.core.event.*

internal class InternalMediatorLiveEvent<T> : wang.lifecycle.MediatorLiveEvent<T>(),
    MediatorLiveEvent<T>,
    MediatorEvent {

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

    @Suppress("UNCHECKED_CAST")
    override fun <S> addSource(source: BackgroundLiveEvent<S>, onChanged: BackgroundObserver<in S>) {
        super.addSource(source as wang.lifecycle.BackgroundLiveEvent<S>, onChanged)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S> removeSource(toRemote: BackgroundLiveEvent<S>) {
        super.removeSource(toRemote as wang.lifecycle.BackgroundLiveEvent<S>,)
    }

    @MainThread
    override fun <S> addLiveDataSource(source: LiveData<S>, onChanged: Observer<in S>) {
        super.addSource(source, onChanged)
    }

    @MainThread
    override fun <S> removeLiveDataSource(toRemote: LiveData<S>) {
        super.removeSource(toRemote)
    }
}
