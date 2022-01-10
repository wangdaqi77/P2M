package com.p2m.core.channel

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.p2m.core.exception.P2MException
import com.p2m.core.launcher.ILaunchActivityInterceptor
import kotlin.reflect.KClass

interface InterceptorService {
    fun doInterceptions(
        channel: Channel,
        interceptors: Collection<IInterceptor>,
        callback: InterceptorCallback
    )
}

internal class InterceptorServiceDefault : InterceptorService {

    override fun doInterceptions(
        channel: Channel,
        interceptors: Collection<IInterceptor>,
        callback: InterceptorCallback
    ) {
        val interceptorIterator = interceptors.iterator()
        try {
            doInterception(
                interceptorIterator = interceptorIterator,
                channel = channel,
                onContinue = { callback.onContinue() },
                onRedirect = { callback.onRedirect(it) },
                onInterrupted = { e -> callback.onInterrupt(e) }
            )
        } catch (e : Throwable) {
            callback.onInterrupt(e)
        }
    }

    private fun doInterception(
        interceptorIterator: Iterator<IInterceptor>, channel: Channel,
        onContinue: () -> Unit,
        onRedirect: (redirectChannel: Channel) -> Unit,
        onInterrupted: (e: Throwable) -> Unit
    ) {
        if (interceptorIterator.hasNext()) {
            val interceptor = interceptorIterator.next()
            interceptor.process(channel, object : InterceptorCallback {
                override fun onContinue() {
                    doInterception(interceptorIterator, channel, onContinue, onRedirect, onInterrupted)
                }

                override fun onRedirect(redirectChannel: Channel) {
                    onRedirect(redirectChannel)
                }

                override fun onInterrupt(e: Throwable?) {
                    onInterrupted(e ?: P2MException("No message."))
                }
            })
        } else {
            onContinue()
        }
    }
}

/**
 * A callback when launch been intercepted.
 */
interface InterceptorCallback {
    fun onContinue()

    fun onRedirect(redirectChannel: Channel)

    fun onInterrupt(e: Throwable? = null)
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