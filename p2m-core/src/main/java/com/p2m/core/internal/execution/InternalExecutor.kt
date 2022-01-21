package com.p2m.core.internal.execution

import android.os.Handler
import android.os.HandlerThread

internal class InternalExecutor : Executor {
    private val handlerThread = HandlerThread("p2m_internal")
    private val handler by lazy { Handler(handlerThread.looper) }

    init {
        loop()
    }

    override fun loop() {
        handlerThread.start()
    }

    override fun executeTask(runnable: Runnable) {
        if (Thread.currentThread() === handlerThread) {
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
        handlerThread.quit()
    }
}