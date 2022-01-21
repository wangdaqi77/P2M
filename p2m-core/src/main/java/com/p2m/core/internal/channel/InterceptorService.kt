package com.p2m.core.internal.channel

import com.p2m.core.channel.*
import com.p2m.core.exception.P2MException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit


internal class InterceptorServiceDefault(private val executor: ExecutorService) :
    InterceptorService {

    override fun doInterceptions(
        channel: Channel,
        interceptors: Collection<IInterceptor>,
        callback: InterceptorServiceCallback
    ) {
        executor.execute {
            val countDownLatch = CountDownLatch(interceptors.size)
            val interceptorIterator = interceptors.iterator()
            var processed = false
            val complete = {
                processed = true
                while (countDownLatch.count > 0) {
                    countDownLatch.countDown()
                }
            }
            doInterception(
                interceptorIterator = interceptorIterator,
                channel = channel,
                countDownLatch,
                onInterceptorProcessing = { interceptor ->
                    callback.onInterceptorProcessing(interceptor)
                },
                onContinue = {
                    callback.onContinue()
                    complete()
                },
                onRedirect = { redirectChannel ->
                    callback.onRedirect(redirectChannel)
                    complete()
                },
                onInterrupted = { e ->
                    callback.onInterrupt(e)
                    complete()
                }
            )

            countDownLatch.await(channel.timeout, TimeUnit.MILLISECONDS)
            when {
                processed -> { }
                interceptorIterator.hasNext() -> callback.onInterrupt(P2MException("process timed out."))
            }
        }
    }

    private fun doInterception(
        interceptorIterator: Iterator<IInterceptor>,
        channel: Channel,
        countDownLatch: CountDownLatch,
        onInterceptorProcessing: (interceptor: IInterceptor) -> Unit,
        onContinue: () -> Unit,
        onRedirect: (redirectChannel: Channel) -> Unit,
        onInterrupted: (e: Throwable) -> Unit
    ) {
        if (interceptorIterator.hasNext()) {
            val interceptor = interceptorIterator.next()
            onInterceptorProcessing(interceptor)
            interceptor.process(channel, object : InterceptorCallback {
                override fun onContinue() {
                    countDownLatch.countDown()
                    doInterception(
                        interceptorIterator,
                        channel,
                        countDownLatch,
                        onInterceptorProcessing,
                        onContinue,
                        onRedirect,
                        onInterrupted
                    )
                }

                override fun onRedirect(redirectChannel: Channel) {
                    onRedirect(redirectChannel)
                }

                override fun onInterrupt(e: Throwable?) {
                    onInterrupted(e ?: P2MException("no message."))
                }
            })
        } else {
            onContinue()
        }
    }
}