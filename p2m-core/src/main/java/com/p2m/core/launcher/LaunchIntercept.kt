package com.p2m.core.launcher

import java.util.*
import kotlin.collections.ArrayList

interface InterceptorService {
    fun doInterceptions(channel: InterceptableChannel, callback: InterceptorCallback)
}

object InterceptorServiceDefault:InterceptorService {

    override fun doInterceptions(channel: InterceptableChannel, callback: InterceptorCallback) {
        // before interceptors -> owner interceptors -> after interceptors
        @Suppress("UNCHECKED_CAST")
        val interceptors = channel.interceptors.toArray() as Array<IInterceptor<InterceptableChannel>> + arrayOf()
        val interceptorIterator = interceptors.iterator()
        try {
            doInterception(interceptorIterator, channel)
            if (interceptorIterator.hasNext()) {
                callback.onInterrupt(channel.tag as? Throwable)
            }else {
                callback.onContinue(channel)
            }
        } catch (e : Throwable) {
            callback.onInterrupt(e)
        }
    }

    private fun doInterception(interceptorIterator: Iterator<IInterceptor<InterceptableChannel>>, blockOrigin: InterceptableChannel) {
        if (interceptorIterator.hasNext()) {
            val interceptor = interceptorIterator.next()
            interceptor.process(blockOrigin, object : InterceptorCallback {
                override fun onContinue(channel: InterceptableChannel) {
                    doInterception(interceptorIterator, channel)
                }

                override fun onInterrupt(e: Throwable?) {
                    blockOrigin.tag = e ?: IllegalStateException("No message.")
                }
            })
        }
    }

}

/**
 * A callback when launch been intercepted.
 */
interface InterceptorCallback {
    fun onContinue(channel: InterceptableChannel)

    fun onInterrupt(e: Throwable? = null)
}

interface LaunchInterceptor : IInterceptor<LaunchChannel>

interface IInterceptor<C : InterceptableChannel> {
    fun process(channel: InterceptableChannel, callback: InterceptorCallback)
}

/**
 * A callback when launch been intercepted.
 */
interface OnIntercept {
    fun onIntercept()
}

typealias Channel = () -> Unit

open class SafeChannel(internal var channel: Channel) {
    private var onFailure : ((channel: SafeChannel) -> Unit)? = null

    protected open fun onFailure(failureBlock: (channel: SafeChannel) -> Unit): SafeChannel {
        this.onFailure = failureBlock
        return this
    }

    protected open fun invoke() {
        try {
            channel()
        } catch (e: Throwable) {
            val failureBlock = onFailure
            if (failureBlock != null) {
                failureBlock(this)
            } else {
                // 降级处理

            }
        }
    }
}

open class InterceptableChannel(
    private val owner: Any,
    channel: Channel,
) : SafeChannel(channel) {

    companion object {
        const val DEFAULT_TIMEOUT = 10_000L
        const val DEFAULT_CHANNEL_INTERCEPT = true
    }

    private var interceptBlock : ((block: InterceptableChannel) -> Unit)? = null
    internal var timeout :Long = DEFAULT_TIMEOUT
    internal var isGreenChannel: Boolean = !DEFAULT_CHANNEL_INTERCEPT
    internal var tag : Any? = null
    internal var interceptors: ArrayList<IInterceptor<InterceptableChannel>> = arrayListOf()

    internal fun interceptors(interceptors: ArrayList<IInterceptor<InterceptableChannel>>): InterceptableChannel {
        this.interceptors.clear()
        this.interceptors += interceptors
        return this
    }

    protected open fun timeout(timeout: Long): InterceptableChannel {
        this.timeout = timeout
        return this
    }

    protected open fun greenChannel(): InterceptableChannel{
        this.isGreenChannel = true
        return this
    }

    protected open fun onIntercept(interceptBlock: (block: InterceptableChannel) -> Unit): InterceptableChannel {
        this.interceptBlock = interceptBlock
        return this
    }

    protected open fun onFailure(failureBlock: (channel: InterceptableChannel) -> Unit): InterceptableChannel {
        return super.onFailure { failureBlock(it as InterceptableChannel) } as InterceptableChannel
    }

    override fun invoke() {
        val superInvoke = {
            super.invoke()
        }

        if (!isGreenChannel) {
            InterceptorServiceDefault.doInterceptions(this, object : InterceptorCallback {
                override fun onContinue(channel: InterceptableChannel) {
                    if (channel === this@InterceptableChannel) {
                        superInvoke()
                    } else {
                        channel.invoke()
                    }
                }

                override fun onInterrupt(e: Throwable?) {
                    val interceptBlock = interceptBlock
                    if (interceptBlock != null) {
                        interceptBlock(this@InterceptableChannel)
                    }
                }

            })
        } else {
            superInvoke()
        }
    }

    override fun hashCode(): Int {
        return owner.hashCode()
    }

    override fun toString(): String {
        return owner.toString()
    }

    override fun equals(other: Any?): Boolean {
        return owner == other
    }
}

interface ReLauncher {
    fun getLauncher(): Launcher

    fun invoke()
}

internal class InternalRelaunch(builder: Builder) : ReLauncher {

    companion object {
        fun newBuilder(launcher: Launcher) = Builder(launcher)
    }

    private var relaunch: Recall = builder.relaunch
    private var launcher: Launcher = builder.launcher

    override fun getLauncher(): Launcher = launcher

    override fun invoke() = relaunch.invoke()

    class Builder(val launcher: Launcher) {
        lateinit var relaunch: Recall

        fun launchBlock(block: () -> Unit): Builder {
            this.relaunch = InternalRecall(block)
            return this
        }

        fun build() = InternalRelaunch(this)
    }
}

interface Recall {
    fun invoke()
}

internal class InternalRecall(private val block: () -> Unit) : Recall {

    override fun invoke() {
        block()
    }
}