package com.p2m.core.internal.execution

internal class TagRunnable(val tag: Any, private val block: () -> Unit) : Runnable {
    override fun run() {
        block()
    }
}