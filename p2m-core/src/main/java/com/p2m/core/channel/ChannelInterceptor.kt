package com.p2m.core.channel

interface InterceptorService {
    fun doInterceptions(
        channel: Channel,
        interceptors: Array<IInterceptor>,
        callback: InterceptorCallback
    )
}

object InterceptorServiceDefault : InterceptorService {

    override fun doInterceptions(
        channel: Channel,
        interceptors: Array<IInterceptor>,
        callback: InterceptorCallback
    ) {
        // before interceptors -> owner interceptors -> after interceptors
        @Suppress("UNCHECKED_CAST")
        val interceptorIterator = interceptors.iterator()
        try {
            doInterception(interceptorIterator, channel)
            if (interceptorIterator.hasNext()) {
                callback.onInterrupt(channel.tag as? Throwable)
            } else {
                callback.onContinue(channel)
            }
        } catch (e : Throwable) {
            callback.onInterrupt(e)
        }
    }

    private fun doInterception(interceptorIterator: Iterator<IInterceptor>, channel: Channel) {
        if (interceptorIterator.hasNext()) {
            val interceptor = interceptorIterator.next()
            interceptor.process(channel, object : InterceptorCallback {
                override fun onContinue(channel: Channel) {
                    doInterception(interceptorIterator, channel)
                }

                override fun onInterrupt(e: Throwable?) {
                    channel.tag = e ?: IllegalStateException("No message.")
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
