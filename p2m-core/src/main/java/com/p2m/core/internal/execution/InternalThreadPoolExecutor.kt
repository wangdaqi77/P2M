package com.p2m.core.internal.execution

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

internal class InternalThreadPoolExecutor : ThreadPoolExecutor(
    CORE_SIZE,
    CORE_SIZE,
    10,
    TimeUnit.SECONDS,
    LinkedBlockingQueue(),
    object : ThreadFactory {
        private val THREAD_NAME_PREFIX = "p2m_internal"
        private val threadId = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            Runtime.getRuntime().availableProcessors()
            val t = Thread(r)
            t.isDaemon = false
            t.priority = Thread.NORM_PRIORITY
            t.name = "${THREAD_NAME_PREFIX}_${threadId.getAndIncrement()}"
            return t
        }
    }
) {

    companion object {
        private val CORE_SIZE = min(4, max(2, Runtime.getRuntime().availableProcessors() + 1))
    }

    override fun beforeExecute(t: Thread?, r: Runnable) {
        super.beforeExecute(t, r)
//        if (r is TagRunnable) {
//            logI("beforeExecute for ${r.tag}")
//        }
    }

    override fun afterExecute(r: Runnable, t: Throwable?) {
        super.afterExecute(r, t)
//        if (r is TagRunnable) {
//            logI("afterExecute for ${r.tag}")
//        }
    }
}