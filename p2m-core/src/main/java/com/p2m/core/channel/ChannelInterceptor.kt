package com.p2m.core.channel

import com.p2m.core.exception.P2MException

interface InterceptorService {
    fun doInterceptions(
        channel: Channel,
        interceptors: Array<IInterceptor>,
        callback: InterceptorCallback
    )
}

internal class InterceptorServiceDefault : InterceptorService {

    override fun doInterceptions(
        channel: Channel,
        interceptors: Array<IInterceptor>,
        callback: InterceptorCallback
    ) {
        // before interceptors -> owner interceptors -> after interceptors
        @Suppress("UNCHECKED_CAST")
        val interceptorIterator = interceptors.iterator()
        try {
            doInterception(interceptorIterator, channel) { e ->
                callback.onInterrupt(e)
            }

            if (!interceptorIterator.hasNext()) {
                callback.onContinue(channel)
            }
        } catch (e : Throwable) {
            callback.onInterrupt(e)
        }
    }

    private fun doInterception(interceptorIterator: Iterator<IInterceptor>, channel: Channel, onInterrupted: (e: Throwable) -> Unit) {
        if (interceptorIterator.hasNext()) {
            val interceptor = interceptorIterator.next()
            interceptor.process(channel, object : InterceptorCallback {
                override fun onContinue(channel: Channel) {
                    doInterception(interceptorIterator, channel, onInterrupted)
                }

                override fun onInterrupt(e: Throwable?) {
                    onInterrupted(e ?: P2MException("No message."))
                }
            })
        }
    }
}

/**
 * A callback when launch been intercepted.
 */
interface InterceptorCallback {
    fun onContinue(channel: Channel)

    fun onInterrupt(e: Throwable? = null)
}

interface IInterceptor {
    fun process(channel: Channel, callback: InterceptorCallback)
}
