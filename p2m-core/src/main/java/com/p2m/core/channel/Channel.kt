package com.p2m.core.channel

import com.p2m.core.exception.ChannelRedirectInterruptedException
import com.p2m.core.exception.ChannelClosedException
import com.p2m.core.exception.P2MException
import com.p2m.core.internal._P2M
import com.p2m.core.internal.log.logW
import com.p2m.core.launcher.ActivityLauncher
import com.p2m.core.launcher.LaunchActivityChannel
import com.p2m.core.launcher.Launcher

typealias ChannelBlock = (channel: Channel) -> Unit

interface NavigationCallback {
    fun onStarted(channel: Channel)

    fun onFailure(channel: Channel, e: Throwable)

    fun onCompleted(channel: Channel)

    fun onRedirect(channel: Channel, redirectChannel: Channel)

    fun onInterrupt(channel: Channel, e: Throwable?)
}

open class SimpleNavigationCallback: NavigationCallback {
    override fun onStarted(channel: Channel) {

    }

    override fun onFailure(channel: Channel, e: Throwable) {

    }

    override fun onCompleted(channel: Channel) {

    }

    override fun onRedirect(channel: Channel, redirectChannel: Channel) {

    }

    override fun onInterrupt(channel: Channel, e: Throwable?) {

    }
}

enum class ChannelRedirectionMode {
    NEVER,                          // never redirect, will interrupted if redirect.
    CONTINUOUS_AND_RECOVER_TRY,     // continuous redirects and try recover navigation after redirected.
    CONTINUOUS_AND_RECOVER_FORCE,   // continuous redirects and recover after redirected until navigation completed or failure.
}

open class GreenChannel internal constructor(owner: Any, channelBlock: ChannelBlock) :
    Channel(owner = owner, channelBlock = channelBlock) {

    init {
        greenChannel()
    }

    public override fun onStarted(onStarted: (channel: Channel) -> Unit): GreenChannel {
        return super.onStarted(onStarted) as GreenChannel
    }

    public override fun onFailure(onFailure: (channel: Channel) -> Unit): GreenChannel {
        return super.onFailure(onFailure) as GreenChannel
    }

    public override fun onCompleted(onCompleted: (channel: Channel) -> Unit): GreenChannel {
        return super.onCompleted(onCompleted) as GreenChannel
    }

    public override fun navigation(navigationCallback: NavigationCallback?) {
        super.navigation(navigationCallback)
    }
}

class LaunchGreenChannel internal constructor(
    val launcher: Launcher,
    channelBlock: ChannelBlock
) : GreenChannel(launcher, channelBlock)

open class InterruptibleChannel internal constructor(
    owner: Any,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) :
    Channel(owner = owner, interceptorService = interceptorService, channelBlock = channelBlock) {

    public override fun navigationCallback(navigationCallback: NavigationCallback): InterruptibleChannel {
        return super.navigationCallback(navigationCallback) as InterruptibleChannel
    }

    public override fun onStarted(onStarted: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onStarted(onStarted) as InterruptibleChannel
    }

    public override fun onFailure(onFailure: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onFailure(onFailure) as InterruptibleChannel
    }

    public override fun onCompleted(onCompleted: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onCompleted(onCompleted) as InterruptibleChannel
    }

    public override fun onRedirect(onRedirect: (newChannel: Channel) -> Unit): InterruptibleChannel {
        return super.onRedirect(onRedirect) as InterruptibleChannel
    }

    public override fun onInterrupt(onInterrupt: (channel: Channel, e: Throwable?) -> Unit): InterruptibleChannel {
        return super.onInterrupt(onInterrupt) as InterruptibleChannel
    }

    public override fun timeout(timeout: Long): InterruptibleChannel {
        return super.timeout(timeout) as InterruptibleChannel
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
        private const val DEFAULT_CHANNEL_INTERCEPT = true
        private val EMPTY_BLOCK = { _: Channel -> }

        internal fun green(owner: Any, channelBlock: ChannelBlock) =
            GreenChannel(owner, channelBlock)

        internal fun interruptible(owner: Any, interceptorService: InterceptorService, channelBlock: ChannelBlock) =
            InterruptibleChannel(owner, interceptorService, channelBlock)
                .redirectionMode(ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_TRY)

        internal fun launchActivity(activityLauncher: ActivityLauncher<*, *>, channelBlock: (channel: LaunchActivityChannel) -> Unit) =
            LaunchActivityChannel(activityLauncher, _P2M.launchActivityHelper.interceptorService) {
                _P2M.mainExecutor.postTask(Runnable {
                    channelBlock(it as LaunchActivityChannel)
                })
            }
                .redirectionMode(ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_TRY)

        internal fun launchGreen(launcher: Launcher, channelBlock: ChannelBlock) =
            LaunchGreenChannel(launcher, channelBlock)
    }

    @Volatile
    protected var immutable = false
    private var onStarted : ((channel: Channel) -> Unit)? = null
    private var onFailure : ((channel: Channel) -> Unit)? = null
    private var onCompleted : ((channel: Channel) -> Unit)? = null
    private var onRedirect : ((newChannel: Channel) -> Unit)? = null
    private var onInterrupt : ((channel: Channel, e: Throwable?) -> Unit)? = null
    private var timeout :Long = DEFAULT_TIMEOUT
    private var isGreenChannel: Boolean = !DEFAULT_CHANNEL_INTERCEPT
    private var interceptors: Collection<IInterceptor>? = null
    @Volatile
    private var isCompleted = false
    @Volatile
    private var isClosed = false
    private var closedException : ChannelClosedException? = null
    private val lock = Any()
    private var navigationCallback: NavigationCallback? = null
    internal lateinit var redirectionMode : ChannelRedirectionMode
    protected var onPrepareRedirect : ((redirectChannel: Channel, processingInterceptor: IInterceptor, unprocessedInterceptors: ArrayList<IInterceptor>) -> Unit)? = null
    internal var isRedirectChannel = false
    private val _navigationCallback: NavigationCallback by lazy(LazyThreadSafetyMode.NONE) {
        object : NavigationCallback {
            override fun onStarted(channel: Channel) {
                onStarted?.invoke(channel)
            }

            override fun onFailure(channel: Channel, e: Throwable) {
                onFailure?.invoke(channel)
            }

            override fun onCompleted(channel: Channel) {
                onCompleted?.invoke(channel)
            }

            override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                onRedirect?.invoke(redirectChannel)
            }

            override fun onInterrupt(channel: Channel, e: Throwable?) {
                onInterrupt?.invoke(channel, e)
            }
        }
    }

    private class InternalInterceptorServiceCallback constructor(
        private val channel: Channel,
        private val processingInterceptors: ArrayList<IInterceptor>,
        private val lastRedirectedChannel: Channel? = null,
        private val lastRedirectedInterceptor: IInterceptor? = null
    ): InterceptorServiceCallback {
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
            when (this.channel.redirectionMode) {
                ChannelRedirectionMode.NEVER -> onInterrupt(ChannelRedirectInterruptedException(redirectChannel, "never redirect, redirection mode is ChannelRedirectionMode.NEVER"))
                ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_TRY -> {
                    if (this.lastRedirectedInterceptor === processingInterceptor &&  this.lastRedirectedChannel?.owner === redirectChannel.owner) {
                        onInterrupt(ChannelRedirectInterruptedException(redirectChannel, "cannot be redirected to again, redirection mode is ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_TRY"))
                        return
                    }
                    this.channel.redirectTo(redirectChannel, processingInterceptor, unprocessedInterceptors)
                }
                ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_FORCE -> this.channel.redirectTo(redirectChannel, processingInterceptor, unprocessedInterceptors)
            }
        }

        override fun onInterrupt(e: Throwable?) {
            this.channel.navigationCallback?.onInterrupt(this.channel, e)
        }
    }

    private fun setNavigationCallbackIfNull() {
        if (this.navigationCallback != null && this.navigationCallback !== this._navigationCallback) {
            logW("Called `callback(NavigationCallback)` already.")
        }
        this.navigationCallback = this._navigationCallback
    }

    protected open fun navigationCallback(navigationCallback: NavigationCallback): Channel {
        checkImmutable()
        this.navigationCallback = navigationCallback
        return this
    }

    protected open fun onStarted(onStarted: (channel: Channel) -> Unit): Channel {
        checkImmutable()
        setNavigationCallbackIfNull()
        this.onStarted = onStarted
        return this
    }

    protected open fun onFailure(onFailure: (channel: Channel) -> Unit): Channel {
        checkImmutable()
        setNavigationCallbackIfNull()
        this.onFailure = onFailure
        return this
    }

    protected open fun onCompleted(onCompleted: (channel: Channel) -> Unit): Channel {
        checkImmutable()
        setNavigationCallbackIfNull()
        this.onCompleted = onCompleted
        return this
    }

    protected open fun onInterrupt(onInterrupt: (channel: Channel, e: Throwable?) -> Unit): Channel {
        checkImmutable()
        setNavigationCallbackIfNull()
        this.onInterrupt = onInterrupt
        return this
    }

    protected open fun onRedirect(onRedirect: (newChannel: Channel) -> Unit): Channel {
        checkImmutable()
        setNavigationCallbackIfNull()
        this.onRedirect = onRedirect
        return this
    }

    internal open fun interceptors(interceptors: Collection<IInterceptor>): Channel {
        checkImmutable()
        this.interceptors = interceptors
        return this
    }

    protected open fun timeout(timeout: Long): Channel {
        checkImmutable()
        this.timeout = timeout
        return this
    }

    protected fun greenChannel(): Channel{
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
     *  - Call [NavigationCallback.onFailure] when [channelBlock] is failure.
     *  - Call [NavigationCallback.onCompleted] when [channelBlock] is finished.
     *  - Call [NavigationCallback.onRedirect] when redirected by a interceptor.
     *  - Call [NavigationCallback.onInterrupt] when interrupted by a interceptor.
     */
    protected open fun navigation(navigationCallback: NavigationCallback? = null) {
        synchronized(lock) {
            immutable = true
            if (isCompleted) {
                throw P2MException("Navigation be completed already, not allowed to again.")
            }

            if (navigationCallback != null) {
                if (this@Channel.navigationCallback === _navigationCallback) {
                    logW("Called `onStarted { }` or `onFailure { }` or `onCompleted { }` or `onInterrupt { }` already.")
                }
                this@Channel.navigationCallback = navigationCallback
            }

            this@Channel.navigationCallback?.onStarted(this)

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
                    callback = InternalInterceptorServiceCallback(this, ArrayList(interceptors)))
            } else {
                _navigation(this@Channel._navigationCallback)
            }
        }
    }

    private fun redirectTo(
        redirectChannel: Channel,
        processingInterceptor: IInterceptor,
        unprocessedInterceptors: ArrayList<IInterceptor>
    ) {
        redirectChannel.isRedirectChannel = true
        this.onPrepareRedirect?.invoke(
            /*redirectChannel = */redirectChannel,
            /*processingInterceptor = */processingInterceptor,
            /*processingInterceptors = */unprocessedInterceptors
        )
        redirectChannel._navigation(object : SimpleNavigationCallback() {
            override fun onFailure(channel: Channel, e: Throwable) {
                super.onFailure(channel, e)
                this@Channel.navigationCallback?.onInterrupt(channel = this@Channel, e = ChannelRedirectInterruptedException(redirectChannel, "redirect failure.", e))
            }

            override fun onCompleted(channel: Channel) {
                super.onCompleted(channel)
                this@Channel.navigationCallback?.onRedirect(channel = this@Channel, redirectChannel = redirectChannel)
            }
        })
    }

    internal fun recoverNavigation(
        redirectChannel: LaunchActivityChannel,
        processingInterceptor: IInterceptor,
        unprocessedInterceptors: ArrayList<IInterceptor>
    ) {
        unprocessedInterceptors.add(0, processingInterceptor)
        when (redirectionMode) {
            ChannelRedirectionMode.NEVER -> { /*never access*/ }
            ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_TRY,
            ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_FORCE-> {
                interceptorService?.doInterceptions(
                    channel = this,
                    interceptors = unprocessedInterceptors,
                    callback = InternalInterceptorServiceCallback(this, ArrayList(unprocessedInterceptors), redirectChannel, processingInterceptor)
                )
            }
        }
    }

    private fun _navigation(navigationCallback: NavigationCallback?) {
        synchronized(lock) {
            try {
                if (isClosed) {
                    navigationCallback?.onFailure(this, closedException!!)
                    return
                }
                channelBlock(this)
                isCompleted = true
                navigationCallback?.onCompleted(this)
            } catch (e: Throwable) {
                navigationCallback?.onFailure(this, e)
            } finally {
                close()
            }
        }
    }
}