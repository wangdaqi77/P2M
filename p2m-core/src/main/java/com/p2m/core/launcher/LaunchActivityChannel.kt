package com.p2m.core.launcher

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.p2m.core.channel.*
import com.p2m.core.internal._P2M
import com.p2m.core.internal.log.logW
import kotlin.reflect.KClass

class LaunchActivityChannel internal constructor(
    val launcher: Launcher,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) : InterruptibleChannel(launcher, interceptorService, channelBlock) {
    private val interceptors = arrayListOf<IInterceptor>()
    internal var recoverableChannel: RecoverableLaunchActivityChannel? = null
    private var allowRestore = true

    init {
        _onRedirect =  { redirectChannel ->
            if (redirectChannel is LaunchActivityChannel) {
                redirectChannel.recoverableChannel = RecoverableLaunchActivityChannel(this)
            } else {
                logW("Not support type: ${redirectChannel::class.java.name}")
            }
        }
    }

    /**
     * Disallow restore when redirect activity finishing, default allow.
     */
    fun disallowRestoreWhenRedirectActivityFinishing(): LaunchActivityChannel {
        checkImmutable()
        this.allowRestore = false
        return this
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

    internal fun addInterceptorBefore(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(0, interceptor)
        return this
    }

    internal fun addInterceptorAfter(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(interceptor)
        return this
    }

    fun addInterceptorBefore(interceptor: ILaunchActivityInterceptor): LaunchActivityChannel {
        checkImmutable()
        interceptors.add(0, ILaunchActivityInterceptor.delegate(interceptor))
        return this
    }

    fun addInterceptorAfter(interceptor: ILaunchActivityInterceptor): LaunchActivityChannel {
        checkImmutable()
        interceptors.add(ILaunchActivityInterceptor.delegate(interceptor))
        return this
    }

    override fun timeout(timeout: Long): LaunchActivityChannel {
        return super.timeout(timeout) as LaunchActivityChannel
    }

    override fun navigation(navigationCallback: NavigationCallback?) {
        if (!immutable) {
            @Suppress("UNCHECKED_CAST")
            interceptors(interceptors)
        }
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

interface ILaunchActivityInterceptor {
    companion object {
        internal fun delegate(real: ILaunchActivityInterceptor): IInterceptor =
            LaunchActivityInterceptorDelegate(real)
    }

    @WorkerThread
    fun init(context: Context)

    @WorkerThread
    fun process(callback: LaunchActivityInterceptorCallback)
}

private class LaunchActivityInterceptorDelegate(private val real: ILaunchActivityInterceptor) : IInterceptor {

    override fun init(context: Context) {
        real.init(context)
    }

    override fun process(channel: Channel, callback: InterceptorCallback) {
        if (channel is LaunchActivityChannel) {
            real.process(object : LaunchActivityInterceptorCallback {
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
}

class RecoverableLaunchActivityChannel internal constructor(private val channel: LaunchActivityChannel) {
    private var restored = false
    fun tryRestore() {
        if (restored) {
            logW("restored for ${channel}.")
            return
        }
        restored = true
        channel.navigation()
    }
}