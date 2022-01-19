package com.p2m.core.internal.execution

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

internal class InternalMainExecutor : Executor {
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    init {
        loop()
    }

    override fun loop() {}

    override fun postTask(runnable: Runnable) {
        handler.post(runnable)
    }

    override fun postTaskDelay(ms: Long, runnable: Runnable) {
        handler.postDelayed(runnable, ms)
    }

    override fun cancelTask(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }

    override fun quitLoop(runnable: Runnable?) {}
}