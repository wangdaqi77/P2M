package com.p2m.core.internal.graph

import com.p2m.core.internal.execution.TagRunnable
import com.p2m.core.internal.log.logI
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class GraphThreadPoolExecutor : ThreadPoolExecutor(
    0,
    Int.MAX_VALUE,
    5,
    TimeUnit.SECONDS,
    SynchronousQueue(),
    object : ThreadFactory {
        private val THREAD_NAME_PREFIX = "p2m_graph"
        private val threadId = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            val t = Thread(r)
            t.isDaemon = false
            t.priority = Thread.NORM_PRIORITY
            t.name = "${THREAD_NAME_PREFIX}_${threadId.getAndIncrement()}"
            return t
        }
    }
) {
    override fun beforeExecute(t: Thread?, r: Runnable) {
        super.beforeExecute(t, r)
        if (r is TagRunnable) {
            logI("beforeExecute for ${r.tag}")
        }
    }

    override fun afterExecute(r: Runnable, t: Throwable?) {
        super.afterExecute(r, t)
        if (r is TagRunnable) {
            logI("afterExecute for ${r.tag}")
        }
    }
}