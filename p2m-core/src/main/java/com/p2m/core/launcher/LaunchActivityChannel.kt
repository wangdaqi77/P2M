package com.p2m.core.launcher

import android.content.Context
import androidx.annotation.WorkerThread
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.channel.*
import com.p2m.core.internal._P2M
import com.p2m.core.P2M
import kotlin.reflect.KClass

class LaunchActivityChannel internal constructor(
    val activityLauncher: ActivityLauncher<*, *>,
    interceptorService: InterceptorService,
    channelBlock: ChannelBlock
) : InterruptibleChannel(activityLauncher, interceptorService, channelBlock) {
    companion object {
        internal fun create(activityLauncher: ActivityLauncher<*, *>, channelBlock: (channel: LaunchActivityChannel) -> Unit) =
            LaunchActivityChannel(activityLauncher, _P2M.interceptorService) {
                _P2M.mainExecutor.executeTask {
                    channelBlock(it as LaunchActivityChannel)
                }
            }
    }

    private var interceptors :ArrayList<IInterceptor>? = null

    /**
     * Add the annotated interceptor before this `activityLauncher.annotatedInterceptorClasses`.
     *
     * Run the following code, interceptors execution order on runtime:
     * `head -> before2 -> before1 -> annotatedInterceptorClasses -> tail`:
     * ```
     * channel.addAnnotatedInterceptorBefore(before1)
     * channel.addAnnotatedInterceptorBefore(before2)
     * ```
     *
     * @see ApiLauncher add `annotatedInterceptorClasses` for this `activityLauncher`.
     * @see P2M.addInterceptorToHead add the interceptor to head for every `ActivityLauncher` instance.
     * @see P2M.addInterceptorToTail add the interceptor to tail for every `ActivityLauncher` instance.
     */
    fun addAnnotatedInterceptorBefore(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()

        val interceptors = this.interceptors ?:  arrayListOf<IInterceptor>().also{
            this.interceptors = it
        }
        val interceptor = _P2M.interceptorContainer.get(interceptorClass)
        interceptors.add(0, interceptor)
        return this
    }

    /**
     * Add the annotated interceptor after this `activityLauncher.annotatedInterceptorClasses`.
     *
     * Run the following code, interceptors execution order on runtime:
     * `head -> annotatedInterceptorClasses -> after1 -> after2 -> tail`:
     * ```
     * channel.addAnnotatedInterceptorBefore(after1)
     * channel.addAnnotatedInterceptorBefore(after2)
     * ```
     *
     * @see ApiLauncher add `annotatedInterceptorClasses` for this `activityLauncher`.
     * @see P2M.addInterceptorToHead add the interceptor to head for every `ActivityLauncher` instance.
     * @see P2M.addInterceptorToTail add the interceptor to tail for every `ActivityLauncher` instance.
     */
    fun addAnnotatedInterceptorAfter(interceptorClass: KClass<out ILaunchActivityInterceptor>): LaunchActivityChannel {
        checkImmutable()

        val interceptors = this.interceptors ?: arrayListOf<IInterceptor>().also {
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
     * @see ApiLaunchActivityInterceptor -  annotation for interceptor
     * @see ChannelRedirectionMode -  redirection behavior
     */
    override fun navigation(navigationCallback: NavigationCallback?) {
        if (!immutable) {
            fillGlobalInterceptors()
            interceptors?.run { interceptors(this) }
            interceptors = null
        }
        super.navigation(navigationCallback)
    }

    private fun fillGlobalInterceptors() {
        val globalHeadForLaunchActivity = _P2M.interceptorContainer.globalHeadForLaunchActivity
        val globalTailForLaunchActivity = _P2M.interceptorContainer.globalTailForLaunchActivity
        if (globalHeadForLaunchActivity.isNotEmpty() || globalTailForLaunchActivity.isNotEmpty()) {
            val interceptors = this.interceptors ?: arrayListOf<IInterceptor>().also {
                this.interceptors = it
            }
            interceptors.addAll(0, globalHeadForLaunchActivity)
            interceptors.addAll(globalTailForLaunchActivity)
        }
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

abstract class GlobalLaunchActivityInterceptor : ILaunchActivityInterceptor {
    lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    @WorkerThread
    final override fun process(callback: LaunchActivityInterceptorCallback) { }

    @WorkerThread
    abstract fun process(activityLauncher: ActivityLauncher<*, *>, callback: LaunchActivityInterceptorCallback)
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

internal class LaunchActivityInterceptorDelegate(private val real: ILaunchActivityInterceptor) : IInterceptor {

    override fun init(context: Context) {
        real.init(context)
    }

    override fun process(channel: Channel, callback: InterceptorCallback) {
        if (channel is LaunchActivityChannel) {

            val launchActivityInterceptorCallback = object : LaunchActivityInterceptorCallback {
                override fun onContinue() {
                    callback.onContinue()
                }

                override fun onRedirect(redirectChannel: LaunchActivityChannel) {
                    callback.onRedirect(redirectChannel)
                }

                override fun onInterrupt(e: Throwable?) {
                    callback.onInterrupt(e)
                }
            }

            val real = real
            if (real is GlobalLaunchActivityInterceptor) {
                val activityLauncher = channel.activityLauncher
                real.process(activityLauncher, launchActivityInterceptorCallback)
                return
            }
            real.process(launchActivityInterceptorCallback)
        } else {
            callback.onContinue()
        }
    }
}