package com.p2m.core.channel

import com.p2m.core.exception.ChannelClosedException
import com.p2m.core.exception.P2MException
import com.p2m.core.internal.log.logW
import com.p2m.core.launcher.Launcher
import kotlin.properties.Delegates

typealias ChannelBlock = () -> Unit

interface NavigationCallback {
    fun onStarted(channel: Channel)

    fun onFailure(channel: Channel, e: Throwable)

    fun onCompleted(channel: Channel)

    fun onInterrupt(channel: Channel)

    fun onChanged(newChannel: Channel)
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


class LaunchChannel internal constructor(
    val launcher: Launcher,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) : InterruptibleChannel(launcher, interceptorService, channelBlock)

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
    private var onProduceRecoverableChannel: ((RecoverableChannel) -> Unit)? = null
    internal var recoverableChannel by Delegates.observable<RecoverableChannel?>(null) { _, _, newValue ->
        newValue?.also {
            onProduceRecoverableChannel?.invoke(it)
        }
    }

    init {
        _onChanged =  { newChannel ->
            if (newChannel is InterruptibleChannel) {
                newChannel.recoverableChannel = RecoverableChannel(this)
            } else {
                logW("Not support type: ${newChannel::class.java.name}")
            }
        }
    }

    internal fun onProduceRecoverableChannel(onProduceRecoverableChannel: (RecoverableChannel) -> Unit) {
        this.onProduceRecoverableChannel = onProduceRecoverableChannel
    }

    override fun onStarted(onStarted: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onStarted(onStarted) as InterruptibleChannel
    }

    public override fun onFailure(onFailure: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onFailure(onFailure) as InterruptibleChannel
    }

    public override fun onInterrupt(onInterrupt: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onInterrupt(onInterrupt) as InterruptibleChannel
    }

    override fun onCompleted(onCompleted: (channel: Channel) -> Unit): InterruptibleChannel {
        return super.onCompleted(onCompleted) as InterruptibleChannel
    }

    override fun onChanged(onChanged: (newChannel: Channel) -> Unit): InterruptibleChannel {
        return super.onChanged(onChanged) as InterruptibleChannel
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
        private val EMPTY_BLOCK = { }

        internal fun green(owner: Any, channelBlock: ChannelBlock) =
            GreenChannel(owner, channelBlock)

        internal fun interruptible(owner: Any, interceptorService: InterceptorService, channelBlock: ChannelBlock) =
            InterruptibleChannel(owner, interceptorService, channelBlock)

        internal fun launch(launcher: Launcher, interceptorService: InterceptorService, channelBlock: ChannelBlock) =
            LaunchChannel(launcher, interceptorService, channelBlock)

        internal fun launchGreen(launcher: Launcher, channelBlock: ChannelBlock) =
            LaunchGreenChannel(launcher, channelBlock)
    }

    protected var _onChanged : ((newChannel: Channel) -> Unit)? = null
    private var onChanged : ((newChannel: Channel) -> Unit)? = null
    private var onStarted : ((channel: Channel) -> Unit)? = null
    private var onFailure : ((channel: Channel) -> Unit)? = null
    private var onCompleted : ((channel: Channel) -> Unit)? = null
    private var onInterrupt : ((channel: Channel) -> Unit)? = null
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

            override fun onInterrupt(channel: Channel) {
                onInterrupt?.invoke(channel)
            }

            override fun onChanged(newChannel: Channel) {
                onChanged?.invoke(newChannel)
            }
        }
    }

    private fun setNavigationCallbackIfNull() {
        if (this.navigationCallback != null && this.navigationCallback !== this._navigationCallback) {
            logW("Called `callback(NavigationCallback)` already.")
        }
        this.navigationCallback = this._navigationCallback
    }

    protected open fun onStarted(onStarted: (channel: Channel) -> Unit): Channel {
        setNavigationCallbackIfNull()
        this.onStarted = onStarted
        return this
    }

    protected open fun onFailure(onFailure: (channel: Channel) -> Unit): Channel {
        setNavigationCallbackIfNull()
        this.onFailure = onFailure
        return this
    }

    protected open fun onCompleted(onCompleted: (channel: Channel) -> Unit): Channel {
        setNavigationCallbackIfNull()
        this.onCompleted = onCompleted
        return this
    }

    protected open fun onInterrupt(onInterrupt: (channel: Channel) -> Unit): Channel {
        setNavigationCallbackIfNull()
        this.onInterrupt = onInterrupt
        return this
    }

    protected open fun onChanged(onChanged: (newChannel: Channel) -> Unit): Channel {
        setNavigationCallbackIfNull()
        this.onChanged = onChanged
        return this
    }

    internal open fun interceptors(interceptors: Array<IInterceptor>): Channel {
        this.interceptors = interceptors
        return this
    }

    protected open fun timeout(timeout: Long): Channel {
        this.timeout = timeout
        return this
    }

    protected fun greenChannel(): Channel{
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

    protected open fun navigation(navigationCallback: NavigationCallback? = null) {

        synchronized(lock) {
            if (isCompleted) {
                throw P2MException("Navigation be completed already, not allowed to again.")
            }

            if (navigationCallback != null) {
                if (this.navigationCallback === _navigationCallback) {
                    logW("Called `onStarted { }` or `onFailure { }` or `onCompleted { }` or `onInterrupt { }` already.")
                }
                this.navigationCallback = navigationCallback
            }

            navigationCallback?.onStarted(this)

            if (!isGreenChannel) {
                if (interceptorService == null) {
                    throw P2MException("has not interceptorService.")
                }
                interceptorService.doInterceptions(
                    channel = this,
                    interceptors = interceptors ?: EMPTY_INTERCEPTORS,
                    callback = object : InterceptorCallback {
                        override fun onContinue(channel: Channel) {
                            if (channel !== this@Channel) {
                                _onChanged?.invoke(/*newChannel = */channel)
                                navigationCallback?.onChanged(newChannel = channel)
                            }
                            channel._navigation()
                        }

                        override fun onInterrupt(e: Throwable?) {
                            navigationCallback?.onInterrupt(this@Channel)
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
                    navigationCallback?.onFailure(this, closedException!!)
                    return
                }
                channelBlock()
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