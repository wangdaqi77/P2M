package com.p2m.core.launcher

import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.WorkerThread
import com.p2m.core.channel.*
import com.p2m.core.internal.log.logW

class LaunchActivityChannel internal constructor(
    val launcher: Launcher,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) : InterruptibleChannel(launcher, interceptorService, channelBlock) {
    internal var recoverableChannel: RecoverableLaunchActivityChannel? = null

    init {
        _onRedirect =  { redirectChannel ->
            if (redirectChannel is LaunchActivityChannel) {
                redirectChannel.recoverableChannel = RecoverableLaunchActivityChannel(this)
            } else {
                logW("Not support type: ${redirectChannel::class.java.name}")
            }
        }
    }

    override fun navigationCallback(navigationCallback: NavigationCallback): LaunchActivityChannel {
        return super.navigationCallback(navigationCallback) as LaunchActivityChannel
    }

    override fun onStarted(onStarted: (channel: Channel) -> Unit): LaunchActivityChannel {
        return super.onStarted(onStarted) as LaunchActivityChannel
    }

    override fun onFailure(onFailure: (channel: Channel) -> Unit): LaunchActivityChannel {
        return super.onFailure(onFailure) as LaunchActivityChannel
    }

    override fun onCompleted(onCompleted: (channel: Channel) -> Unit): LaunchActivityChannel {
        return super.onCompleted(onCompleted) as LaunchActivityChannel
    }

    override fun onRedirect(onRedirect: (newChannel: Channel) -> Unit): LaunchActivityChannel {
        return super.onRedirect(onRedirect) as LaunchActivityChannel
    }

    override fun onInterrupt(onInterrupt: (channel: Channel, e: Throwable?) -> Unit): LaunchActivityChannel {
        return super.onInterrupt(onInterrupt) as LaunchActivityChannel
    }

    override fun timeout(timeout: Long): LaunchActivityChannel {
        return super.timeout(timeout) as LaunchActivityChannel
    }

    override fun navigation(navigationCallback: NavigationCallback?) {
        super.navigation(navigationCallback)
    }
}

interface LaunchActivityInterceptorCallback {
    /**
     * continue
     */
    fun onContinue()

    /**
     * redirect
     */
    fun onRedirect(redirectChannel: LaunchActivityChannel)

    /**
     * interrupt
     */
    fun onInterrupt(e: Throwable? = null)
}

abstract class LaunchActivityInterceptor : Interceptor {
    private lateinit var context: Context

    final override fun process(channel: Channel, callback: InterceptorCallback) {
        if (channel is LaunchActivityChannel) {
            process(object : LaunchActivityInterceptorCallback {
                override fun onContinue() {
                    callback.onContinue()
                }

                override fun onRedirect(redirectChannel: LaunchActivityChannel) {
                    onRedirect(redirectChannel)
                }

                override fun onInterrupt(e: Throwable?) {
                    callback.onInterrupt(e)
                }
            })
        } else {
            callback.onContinue()
        }
    }

    @CallSuper
    override fun init(context: Context) {
        this.context = context.applicationContext
    }

    @WorkerThread
    abstract fun process(callback: LaunchActivityInterceptorCallback)
}

class RecoverableLaunchActivityChannel internal constructor(private val channel: LaunchActivityChannel) {
    private var restored = false
    fun restore() {
        if (restored) {
            logW("restored for ${channel}.")
            return
        }
        restored = true
        channel.navigation()
    }
}