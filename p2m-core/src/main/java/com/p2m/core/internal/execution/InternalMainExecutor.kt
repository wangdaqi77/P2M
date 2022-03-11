package com.p2m.core.internal.execution

import android.os.Handler
import android.os.Looper
import com.p2m.core.internal.log.logW

internal class InternalMainExecutor : Executor {
    private val handler = Handler(Looper.getMainLooper())

    override fun loop() {
        logW("disallow in main thread")
    }

    override fun executeTask(runnable: Runnable) {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            runnable.run()
        } else {
            postTask(runnable)
        }
    }

    override fun postTask(runnable: Runnable) {
        handler.post(runnable)
    }

    override fun postTaskDelay(ms: Long, runnable: Runnable) {
        handler.postDelayed(runnable, ms)
    }

    override fun cancelTask(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }

    override fun quitLoop(runnable: Runnable?) {
        logW("disallow in main thread")
    }
}