package com.p2m.core.launcher

import android.content.Context
import android.content.Intent
import android.app.Service
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.internal.launcher.InternalServiceLauncher
import kotlin.reflect.KProperty

/**
 * A launcher of `Service`.
 *
 * For example, has a `Service` for work in `Account` module:
 * ```kotlin
 * @ApiLauncher("Work")
 * class WorkService : Service()
 * ```
 *
 * launch:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .launcher
 *      .serviceOfWork
 *      .launchChannel(::startService)
 *      .navigation()
 * ```
 * stop:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .launcher
 *      .serviceOfWork
 *      .stop()
 * ```
 *
 * @see Context.startService - e.g.`launchChannel(::startService)`.
 * @see Context.startForegroundService - e.g.`launchChannel(::startForegroundService)`.
 * @see Context.bindService - e.g. use `launchChannel(::xx)` need declare `fun xx(intent: Intent)`
 * method for call bindService.
 * @see ApiLauncher
 */
interface ServiceLauncher : Launcher {
    companion object {
        fun delegate(clazz: Class<*>): Lazy<ServiceLauncher> =
            lazy(LazyThreadSafetyMode.NONE) { InternalServiceLauncher(clazz) }
    }

    /**
     * Will create a launch green channel for that [Service] class uses
     * annotation [ApiLauncher], please call `navigation` to launch.
     *
     * [launchBlock] is real launch method, that has a created Intent instance
     * as input param, all other fields (action, data, type) are null, they can
     * be modified later in [launchBlock].
     *
     * @return [LaunchServiceChannel] - call `navigation` to launch.
     *
     * @see LaunchServiceChannel.navigation
     */
    fun launchChannel(launchBlock: LaunchServiceBlock): LaunchServiceChannel

    /**
     * stop service.
     */
    fun stop(context: Context)
}

/**
 * A block for launch service.
 */
typealias LaunchServiceBlock = (createdIntent: Intent) -> Unit