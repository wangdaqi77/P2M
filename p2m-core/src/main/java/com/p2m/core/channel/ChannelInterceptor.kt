package com.p2m.core.channel

import android.content.Context
import androidx.annotation.WorkerThread
import com.p2m.core.launcher.ILaunchActivityInterceptor
import kotlin.reflect.KClass

interface InterceptorService {
    fun doInterceptions(
        channel: Channel,
        interceptors: Collection<IInterceptor>,
        callback: InterceptorServiceCallback
    )
}

/**
 * A callback when launch been intercepted.
 */
interface InterceptorCallback {
    fun onContinue()

    fun onRedirect(redirectChannel: Channel)

    fun onInterrupt(e: Throwable? = null)
}

interface InterceptorServiceCallback: InterceptorCallback {
    fun onInterceptorProcessing(interceptor: IInterceptor)
}

interface IInterceptor {
    @WorkerThread
    fun init(context: Context)

    @WorkerThread
    fun process(channel: Channel, callback: InterceptorCallback)
}

interface ChannelInterceptorFactory{
    fun createInterceptor(interceptorClass: KClass<IInterceptor>): IInterceptor

    fun createLaunchActivityInterceptor(interceptorClass: KClass<ILaunchActivityInterceptor>): IInterceptor
}