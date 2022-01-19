package com.p2m.core.internal.execution

internal interface Executor{

    fun loop()

    fun postTask(runnable: Runnable)

    fun postTaskDelay(ms:Long, runnable: Runnable)

    fun cancelTask(runnable: Runnable)

    fun quitLoop(runnable: Runnable? = null)
}