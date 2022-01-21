package com.p2m.core.internal.graph

import java.util.concurrent.atomic.AtomicReference

internal abstract class Node<N : Node<N>> {
    abstract val name: String
    abstract val isTop: Boolean

    var byDependDegree: Int = 0
    val byDependNodes: HashSet<N> = HashSet()

    var dependDegree: Int = 0
    val dependNodes: HashSet<N> = HashSet()

    val isExecuted: Boolean
        get() = state.get() == State.COMPLETED

    private val stateLock = Any()
    private val state: AtomicReference<State> = AtomicReference(State.NONE)
    private val completedListeners = arrayListOf<Runnable>()

    private fun addOnCompleted(runner:Runnable) {
        completedListeners.add(runner)
    }

    fun mark(state: State) {
        this.state.set(state)
    }

    fun markCompleted() {
        synchronized(stateLock) {
            state.set(State.COMPLETED)
            for (completedListener in completedListeners) {
                completedListener.run()
            }
        }
    }

    fun markStarted(onComplete: () -> Unit, action: () -> Unit) {
        if (state.compareAndSet(State.NONE, State.STARTED)) {
            action()
            return
        }
        considerNotifyCompleted(onComplete)
    }

    private fun considerNotifyCompleted(onComplete: () -> Unit) {
        if (state.get() == State.COMPLETED) {
            onComplete()
            return
        }
        synchronized(stateLock) {
            if (state.get() == State.COMPLETED) {
                onComplete()
                return
            }
            addOnCompleted(Runnable {
                onComplete()
            })
        }
    }

    enum class State {
        NONE,
        STARTED,
        EVALUATING,
        DEPENDING,
        EXECUTING,
        COMPLETED
    }
}