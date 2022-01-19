package com.p2m.core.launcher

import android.content.Context
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

    init {
        onPrepareRedirect =  { redirectChannel, processingInterceptor, unprocessedInterceptors ->
            if (redirectChannel is LaunchActivityChannel) {
                redirectChannel.recoverableChannel =
                    RecoverableLaunchActivityChannel(
                        this,
                        redirectChannel,
                        processingInterceptor,
                        unprocessedInterceptors
                    )
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

    fun addInterceptorBefore(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(0, interceptor)
        return this
    }

    fun addInterceptorAfter(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(interceptor)
        return this
    }

    override fun timeout(timeout: Long): LaunchActivityChannel {
        return super.timeout(timeout) as LaunchActivityChannel
    }

    override fun redirectionMode(mode: ChannelRedirectionMode): LaunchActivityChannel {
        return super.redirectionMode(mode) as LaunchActivityChannel
    }

    /**
     * Call [channelBlock] when the channel is not interrupted and redirected.
     *
     * If it is redirected, will wait for the redirected `activity` to close
     * and restart the [navigation], you also can disable this feature
     * by calling [restoreMode].
     */
    override fun navigation(navigationCallback: NavigationCallback?) {
        if (!immutable) {
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
                    callback.onRedirect(redirectChannel)
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

class RecoverableLaunchActivityChannel internal constructor(
    private val channel: LaunchActivityChannel,
    private val redirectChannel: LaunchActivityChannel,
    private val processingInterceptor: IInterceptor,
    private val unprocessedInterceptors: ArrayList<IInterceptor>
) {
    fun tryRestoreNavigation() {
        channel.recoverNavigation(redirectChannel, processingInterceptor, unprocessedInterceptors)
    }
}