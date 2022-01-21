package com.p2m.core.launcher

import android.content.Context
import androidx.annotation.WorkerThread
import com.p2m.annotation.module.api.LaunchActivityInterceptor
import com.p2m.core.channel.*
import com.p2m.core.internal._P2M
import kotlin.reflect.KClass

class LaunchActivityChannel internal constructor(
    val launcher: Launcher,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) : InterruptibleChannel(launcher, interceptorService, channelBlock) {
    companion object {
        internal fun create(activityLauncher: ActivityLauncher<*, *>, channelBlock: (channel: LaunchActivityChannel) -> Unit) =
            LaunchActivityChannel(activityLauncher, _P2M.interceptorService) {
                _P2M.mainExecutor.executeTask {
                    channelBlock(it as LaunchActivityChannel)
                }
            }
    }

    private var interceptors :ArrayList<IInterceptor>? = null

    fun addInterceptorBefore(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()

        val interceptors = this.interceptors ?:  arrayListOf<IInterceptor>().also{
            this.interceptors = it
        }
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(0, interceptor)
        return this
    }

    fun addInterceptorAfter(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()

        val interceptors = this.interceptors ?:  arrayListOf<IInterceptor>().also{
            this.interceptors = it
        }
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(interceptor)
        return this
    }

    /**
     * Call [channelBlock] when the channel is not interrupted and redirected.
     *
     * If call [LaunchActivityInterceptorCallback.onRedirect] in a interceptor,
     * will select continue to redirect or interrupted according to different [redirectionMode].
     *
     * @see ILaunchActivityInterceptor - interceptor
     * @see LaunchActivityInterceptorCallback - callback
     * @see LaunchActivityInterceptor -  annotation for interceptor
     * @see ChannelRedirectionMode -  redirection behavior
     */
    override fun navigation(navigationCallback: NavigationCallback?) {
        if (!immutable) {
            interceptors?.run { interceptors(this) }
            interceptors = null
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