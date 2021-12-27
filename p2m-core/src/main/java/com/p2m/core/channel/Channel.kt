package com.p2m.core.channel

import com.p2m.core.exception.ChannelClosedException
import com.p2m.core.internal.log.logW

typealias ChannelBlock = () -> Unit

interface NavigationCallback {
    fun onStarted(channel: Channel)

    fun onFailure(channel: Channel, e: Throwable)

    fun onCompleted(channel: Channel)

    fun onInterrupt(channel: Channel)
}

class GreenChannel internal constructor(owner: Any, channelBlock: ChannelBlock) :
    Channel(owner, channelBlock) {

    init {
        greenChannel()
    }

    public override fun callback(navigationCallback: NavigationCallback) {
        super.callback(navigationCallback)
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

    public override fun navigation() {
        super.navigation()
    }
}


class InterruptibleChannel internal constructor(owner: Any, channelBlock: ChannelBlock) :
    Channel(owner, channelBlock) {

    public override fun callback(navigationCallback: NavigationCallback) {
        super.callback(navigationCallback)
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

    public override fun timeout(timeout: Long): InterruptibleChannel {
        return super.timeout(timeout) as InterruptibleChannel
    }

    public override fun navigation() {
        super.navigation()
    }
}

open class Channel internal constructor(val owner: Any, private var channelBlock: ChannelBlock) {
    companion object {
        private const val DEFAULT_TIMEOUT = 10_000L
        private const val DEFAULT_CHANNEL_INTERCEPT = true
        private val EMPTY_INTERCEPTORS = arrayOf<IInterceptor>()
        private val EMPTY_BLOCK = { }

        internal fun green(owner: Any, channelBlock: ChannelBlock) =
            GreenChannel(owner, channelBlock)

        internal fun interruptible(owner: Any, channelBlock: ChannelBlock) =
            InterruptibleChannel(owner, channelBlock)
    }

    private var onStarted : ((channel: Channel) -> Unit)? = null
    private var onFailure : ((channel: Channel) -> Unit)? = null
    private var onCompleted : ((channel: Channel) -> Unit)? = null
    private var onInterrupt : ((channel: Channel) -> Unit)? = null
    private var timeout :Long = DEFAULT_TIMEOUT
    private  var isGreenChannel: Boolean = !DEFAULT_CHANNEL_INTERCEPT
    private var interceptors: Array<IInterceptor>? = null
    @Volatile
    private var  isStarted = false
    @Volatile
    private var isClosed = false
    private var closedException : ChannelClosedException? = null
    private val lock = Any()
    internal var tag : Any? = null
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
        }
    }

    private fun setNavigationCallbackIfNull() {
        if (this.navigationCallback != null && this.navigationCallback !== this._navigationCallback) {
            logW("Called `callback(NavigationCallback)` already.")
        }
        this.navigationCallback = this._navigationCallback
    }

    protected open fun callback(navigationCallback: NavigationCallback) {
        if (this.navigationCallback === _navigationCallback) {
            logW("Called `onStarted { }` or `onFailure { }` or `onCompleted { }` or `onInterrupt { }` already.")
        }
        this.navigationCallback = navigationCallback
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

    internal open fun interceptors(interceptors: Array<IInterceptor>): Channel {
        this.interceptors = interceptors
        return this
    }

    protected open fun timeout(timeout: Long): Channel {
        this.timeout = timeout
        return this
    }

    protected open fun greenChannel(): Channel{
        this.isGreenChannel = true
        return this
    }

    protected open fun onInterrupt(onInterrupt: (channel: Channel) -> Unit): Channel {
        setNavigationCallbackIfNull()
        this.onInterrupt = onInterrupt
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
                closedException = ChannelClosedException("The channel is closed in $owner.")
            }
        }
    }

    protected open fun navigation() {
        synchronized(lock) {
            if (!isStarted) {
                isStarted = true
                navigationCallback?.onStarted(this)
            }

            if (!isGreenChannel) {
                InterceptorServiceDefault.doInterceptions(this, interceptors ?: EMPTY_INTERCEPTORS, object : InterceptorCallback {
                    override fun onContinue(channel: Channel) {
                        channel._navigation()
                    }

                    override fun onInterrupt(e: Throwable?) {
                        navigationCallback?.onInterrupt(this@Channel)
                    }

                })
                return@synchronized
            }

            _navigation()
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
                navigationCallback?.onCompleted(this)
            } catch (e: Throwable) {
                navigationCallback?.onFailure(this, e)
            } finally {
                close()
            }
        }
    }
}