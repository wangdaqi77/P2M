package com.p2m.core.channel

import com.p2m.core.exception.ChannelRedirectInterruptedException
import com.p2m.core.exception.ChannelClosedException
import com.p2m.core.exception.P2MException
import com.p2m.core.internal._P2M
import com.p2m.core.internal.log.logE
import com.p2m.core.internal.log.logW

typealias ChannelBlock = (channel: Channel) -> Unit

interface NavigationCallback {
    fun onStarted(channel: Channel)

    fun onCompleted(channel: Channel)

    fun onRedirect(channel: Channel, redirectChannel: Channel)

    fun onInterrupt(channel: Channel, e: Throwable)
}

open class SimpleNavigationCallback: NavigationCallback {
    override fun onStarted(channel: Channel) {

    }

    override fun onCompleted(channel: Channel) {

    }

    override fun onRedirect(channel: Channel, redirectChannel: Channel) {

    }

    override fun onInterrupt(channel: Channel, e: Throwable) {

    }
}


/**
 * If call [InterceptorCallback.onRedirect] in a interceptor, will select
 * continue to redirect or interrupted according to different mode.
 */
enum class ChannelRedirectionMode {
    /**
     * Interrupt if redirect.
     *
     * Callback [NavigationCallback.onInterrupt] when interrupted,
     * that exception type is [ChannelRedirectInterruptedException].
     */
    CONSERVATIVE,

    /**
     * Interrupt if redirected to the same channel by the same interceptor
     * when recover navigation.
     *
     * Callback [NavigationCallback.onInterrupt] when interrupted,
     * that exception type is [ChannelRedirectInterruptedException].
     */
    FLEXIBLY,

    /**
     * Redirects until navigation completed.
     */
    RADICAL,
}

open class GreenChannel internal constructor(owner: Any, channelBlock: ChannelBlock) :
    Channel(owner = owner, channelBlock = channelBlock) {

    init {
        greenChannel()
    }

    final override fun greenChannel(): Channel {
        return super.greenChannel()
    }

    public override fun onStarted(onStarted: (channel: Channel) -> Unit): GreenChannel {
        return super.onStarted(onStarted) as GreenChannel
    }

    public override fun onCompleted(onCompleted: (channel: Channel) -> Unit): GreenChannel {
        return super.onCompleted(onCompleted) as GreenChannel
    }

    public override fun navigation(navigationCallback: NavigationCallback?) {
        super.navigation(navigationCallback)
    }
}

open class InterruptibleChannel internal constructor(
    owner: Any,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) :
    Channel(owner = owner, interceptorService = interceptorService, channelBlock = channelBlock) {

    public override fun onStarted(onStarted: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onStarted(onStarted) as InterruptibleChannel
    }

    public override fun onCompleted(onCompleted: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onCompleted(onCompleted) as InterruptibleChannel
    }

    public override fun onInterrupt(onInterrupt: (channel: Channel, e: Throwable?) -> Unit): InterruptibleChannel {
        return super.onInterrupt(onInterrupt) as InterruptibleChannel
    }


    override fun timeout(timeout: Long): InterruptibleChannel {
        return super.timeout(timeout) as InterruptibleChannel
    }

    public override fun greenChannel(): InterruptibleChannel {
        return super.greenChannel() as InterruptibleChannel
    }

    public override fun redirectionMode(mode: ChannelRedirectionMode): InterruptibleChannel {
        return super.redirectionMode(mode) as InterruptibleChannel
    }

    public override fun navigation(navigationCallback: NavigationCallback?) {
        super.navigation(navigationCallback)
    }
}

open class Channel internal constructor(
    val owner: Any,
    private val interceptorService: InterceptorService? = null,
    private var channelBlock: ChannelBlock
) {
    companion object {
        private const val DEFAULT_TIMEOUT = 10_000L
        private val EMPTY_BLOCK = { _: Channel -> /*never*/ }
    }

    protected var immutable = false
    internal var isRedirectChannel = false
    internal var recoverableChannel: RecoverableChannel? = null
    internal var timeout: Long = DEFAULT_TIMEOUT
    private var redirectionMode : ChannelRedirectionMode = ChannelRedirectionMode.FLEXIBLY
    private var onStarted : ((channel: Channel) -> Unit)? = null
    private var onCompleted : ((channel: Channel) -> Unit)? = null
    private var onRedirect : ((newChannel: Channel) -> Unit)? = null
    private var onInterrupt : ((channel: Channel, e: Throwable) -> Unit)? = null
    private var isGreenChannel: Boolean = false
    private var interceptors: Collection<IInterceptor>? = null
    private val lock = Any()
    @Volatile private var isCompleted = false
    @Volatile private var isClosed = false
    private var closedException : ChannelClosedException? = null
    private var navigationCallback: NavigationCallback? = null
    private val _navigationCallback: NavigationCallback by lazy(LazyThreadSafetyMode.NONE) {
        object : NavigationCallback {
            override fun onStarted(channel: Channel) {
                runOnMainThread {
                    onStarted?.invoke(channel)
                    navigationCallback?.onStarted(channel)
                }
            }

            override fun onCompleted(channel: Channel) {
                runOnMainThread {
                    onCompleted?.invoke(channel)
                    navigationCallback?.onCompleted(channel)
                }
            }

            override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                runOnMainThread {
                    onRedirect?.invoke(redirectChannel)
                    navigationCallback?.onRedirect(channel, redirectChannel)
                }
            }

            override fun onInterrupt(channel: Channel, e: Throwable) {
                runOnMainThread {
                    onInterrupt?.invoke(channel, e)
                    navigationCallback?.onInterrupt(channel, e)
                }
            }
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        _P2M.mainExecutor.executeTask {
            block()
        }
    }

    protected open fun onStarted(onStarted: (channel: Channel) -> Unit): Channel {
        checkImmutable()
        this.onStarted = onStarted
        return this
    }

    protected open fun onCompleted(onCompleted: (channel: Channel) -> Unit): Channel {
        checkImmutable()
        this.onCompleted = onCompleted
        return this
    }

    protected open fun onInterrupt(onInterrupt: (channel: Channel, e: Throwable?) -> Unit): Channel {
        checkImmutable()
        this.onInterrupt = onInterrupt
        return this
    }

    protected open fun onRedirect(onRedirect: (newChannel: Channel) -> Unit): Channel {
        checkImmutable()
        this.onRedirect = onRedirect
        return this
    }

    protected fun interceptors(interceptors: Collection<IInterceptor>): Channel {
        checkImmutable()
        this.interceptors = interceptors
        return this
    }

    protected open fun timeout(timeout: Long): Channel {
        checkImmutable()
        this.timeout = timeout
        return this
    }

    protected open fun greenChannel(): Channel{
        checkImmutable()
        this.isGreenChannel = true
        return this
    }

    /**
     * set redirect mode.
     */
    protected open fun redirectionMode(mode: ChannelRedirectionMode): Channel {
        checkImmutable()
        this.redirectionMode = mode
        return this
    }

    /**
     * If the method is called when the channel is already closed, this method will be ignored.
     */
    protected open fun close(cause: Throwable? = null) {
        if (isClosed) return
        synchronized(lock) {
            if (!isClosed) {
                isClosed = true
                channelBlock = EMPTY_BLOCK
                closedException = ChannelClosedException("The channel is closed in $owner.", cause)
            }
        }
    }

    protected fun checkImmutable() {
        if (immutable) throw P2MException("It been immutable, please set before call `navigation`.")
    }

    /**
     * Call [channelBlock] when the channel is not interrupted and redirected.
     *
     * The entire navigation process will be called back:
     *  - Call [NavigationCallback.onStarted] when `navigation` is started.
     *  - Call [NavigationCallback.onCompleted] when [channelBlock] is finished.
     *  - Call [NavigationCallback.onRedirect] when redirected by a interceptor.
     *  - Call [NavigationCallback.onInterrupt] when interrupted by a interceptor.
     */
    protected open fun navigation(navigationCallback: NavigationCallback? = null) {
        if (isCompleted) {
            throw P2MException("Navigation be completed already, not allowed to again.")
        }
        synchronized(lock) {
            if (isCompleted) {
                throw P2MException("Navigation be completed already, not allowed to again.")
            }

            if (!immutable) {
                immutable = true
                this@Channel.navigationCallback = navigationCallback
            }

            this@Channel._navigationCallback.onStarted(this)

            if (!isGreenChannel) {
                if (interceptorService == null) {
                    throw P2MException("has not interceptorService.")
                }

                val interceptors = interceptors
                if (interceptors == null) {
                    _navigation(this@Channel._navigationCallback)
                    return
                }
                interceptorService.doInterceptions(
                    channel = this,
                    interceptors = interceptors,
                    callback = InternalInterceptorServiceCallback(this, interceptors)
                )
            } else {
                _navigation(this@Channel._navigationCallback)
            }
        }
    }

    private fun redirectTo(redirectChannel: Channel) {
        redirectChannel.isRedirectChannel = true
        redirectChannel._navigation(object : SimpleNavigationCallback() {
            override fun onCompleted(channel: Channel) {
                super.onCompleted(channel)
                this@Channel._navigationCallback.onRedirect(channel = this@Channel, redirectChannel = redirectChannel)
            }
        })
    }

    internal fun recoverNavigation(
        interruptedChannel: Channel,
        processingInterceptor: IInterceptor,
        unprocessedInterceptors: ArrayList<IInterceptor>
    ) {
        unprocessedInterceptors.add(0, processingInterceptor)
        when (redirectionMode) {
            ChannelRedirectionMode.CONSERVATIVE,
            ChannelRedirectionMode.FLEXIBLY,
            ChannelRedirectionMode.RADICAL-> {
                interceptorService?.doInterceptions(
                    channel = this,
                    interceptors = unprocessedInterceptors,
                    callback = InternalInterceptorServiceCallback(this, unprocessedInterceptors, interruptedChannel, processingInterceptor)
                )
            }
        }
    }

    private fun _navigation(navigationCallback: NavigationCallback?) {
        if (isClosed) {
            navigationCallback?.onInterrupt(this, closedException!!)
            return
        }
        synchronized(lock) {
            if (isClosed) {
                navigationCallback?.onInterrupt(this, closedException!!)
                return
            }
            try {
                channelBlock(this)
                isCompleted = true
                navigationCallback?.onCompleted(this)
            } finally {
                close()
            }
        }
    }

    private class InternalInterceptorServiceCallback constructor(
        private val channel: Channel,
        interceptors: Collection<IInterceptor>,
        private val lastInterruptedChannel: Channel? = null,
        private val lastInterruptedInterceptor: IInterceptor? = null
    ): InterceptorServiceCallback {
        private val processingInterceptors = ArrayList<IInterceptor>(interceptors)
        private var processingInterceptor: IInterceptor? = null
        override fun onInterceptorProcessing(interceptor: IInterceptor) {
            this.processingInterceptors.remove(interceptor)
            this.processingInterceptor = interceptor
        }

        override fun onContinue() {
            this.channel._navigation(this.channel._navigationCallback)
        }

        override fun onRedirect(redirectChannel: Channel) {
            val processingInterceptor = this.processingInterceptor
                ?: throw IllegalStateException("please call `callback.onInterceptorProcessing()` first in service.")
            val unprocessedInterceptors = processingInterceptors
            val recoverableChannel = RecoverableChannel(
                this.channel,
                redirectChannel,
                processingInterceptor,
                unprocessedInterceptors
            )
            redirectChannel.recoverableChannel = recoverableChannel
            when (this.channel.redirectionMode) {
                ChannelRedirectionMode.CONSERVATIVE -> onInterrupt(ChannelRedirectInterruptedException(ChannelRedirectionMode.CONSERVATIVE, recoverableChannel))
                ChannelRedirectionMode.FLEXIBLY -> {
                    if (this.lastInterruptedInterceptor === processingInterceptor &&  this.lastInterruptedChannel?.owner === redirectChannel.owner) {
                        onInterrupt(ChannelRedirectInterruptedException(ChannelRedirectionMode.FLEXIBLY, recoverableChannel))
                        return
                    }
                    this.channel.redirectTo(redirectChannel)
                }
                ChannelRedirectionMode.RADICAL -> this.channel.redirectTo(redirectChannel)
            }
        }

        override fun onInterrupt(e: Throwable?) {
            this.channel._navigationCallback.onInterrupt(this.channel, e ?: P2MException("no message."))
        }
    }
}

/**
 * Channel navigation can be recovered where it left interrupted.
 */
class RecoverableChannel internal constructor(
    val channel: Channel,
    val interruptedChannel: Channel,
    private val processingInterceptor: IInterceptor,
    private val unprocessedInterceptors: ArrayList<IInterceptor>
) {
    fun recoverNavigation() {
        channel.recoverNavigation(interruptedChannel, processingInterceptor, unprocessedInterceptors)
    }
}