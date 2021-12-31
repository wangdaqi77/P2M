package com.p2m.core.channel

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.p2m.core.exception.P2MException

interface InterceptorService {
    fun doInterceptions(
        channel: Channel,
        interceptors: Array<Interceptor>,
        callback: InterceptorCallback
    )
}

internal class InterceptorServiceDefault : InterceptorService {

    override fun doInterceptions(
        channel: Channel,
        interceptors: Array<Interceptor>,
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
        interceptorIterator: Iterator<Interceptor>, channel: Channel,
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

interface Interceptor {
    @MainThread
    fun init(context: Context)

    @WorkerThread
    fun process(channel: Channel, callback: InterceptorCallback)
}
