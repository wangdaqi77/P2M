package com.p2m.core.channel

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

    fun onRedirect(redirectChannel: Channel)

    fun onInterrupt(channel: Channel, e: Throwable?)
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
        private val EMPTY_INTERCEPTORS = arrayOf<IInterceptor>()
        private val EMPTY_BLOCK = { _: Channel -> }

        internal fun green(owner: Any, channelBlock: ChannelBlock) =
            GreenChannel(owner, channelBlock)

        internal fun interruptible(owner: Any, interceptorService: InterceptorService, channelBlock: ChannelBlock) =
            InterruptibleChannel(owner, interceptorService, channelBlock)

        internal fun launchActivity(activityLauncher: ActivityLauncher<*, *>, channelBlock: (channel: LaunchActivityChannel) -> Unit) =
            LaunchActivityChannel(activityLauncher, _P2M.launchActivityHelper.interceptorService) {
                channelBlock(it as LaunchActivityChannel)
            }

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
    private var interceptors: Array<IInterceptor>? = null
    @Volatile
    private var isCompleted = false
    @Volatile
    private var isClosed = false
    private var closedException : ChannelClosedException? = null
    private val lock = Any()
    private var navigationCallback: NavigationCallback? = null
    protected var _onRedirect : ((redirectChannel: Channel) -> Unit)? = null
    internal var isRedirect = false
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

            override fun onRedirect(redirectChannel: Channel) {
                onRedirect?.invoke(redirectChannel)
            }

            override fun onInterrupt(channel: Channel, e: Throwable?) {
                onInterrupt?.invoke(channel, e)
            }
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

    internal open fun interceptors(interceptors: Array<IInterceptor>): Channel {
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
                interceptorService.doInterceptions(
                    channel = this,
                    interceptors = interceptors ?: EMPTY_INTERCEPTORS,
                    callback = object : InterceptorCallback {
                        override fun onContinue() {
                            _navigation()
                        }

                        override fun onRedirect(redirectChannel: Channel) {
                            _onRedirect?.invoke(/*redirectChannel = */redirectChannel)
                            redirectChannel.isRedirect = true
                            this@Channel.navigationCallback?.onRedirect(redirectChannel = redirectChannel)
                            redirectChannel.navigation()
                        }

                        override fun onInterrupt(e: Throwable?) {
                            this@Channel.navigationCallback?.onInterrupt(this@Channel, e)
                        }

                    })
            } else {
                _navigation()
            }
        }
    }

    private fun _navigation() {
        synchronized(lock) {
            try {
                if (isClosed) {
                    this@Channel.navigationCallback?.onFailure(this, closedException!!)
                    return
                }
                channelBlock(this)
                isCompleted = true
                this@Channel.navigationCallback?.onCompleted(this)
            } catch (e: Throwable) {
                this@Channel.navigationCallback?.onFailure(this, e)
            } finally {
                close()
            }
        }
    }
}