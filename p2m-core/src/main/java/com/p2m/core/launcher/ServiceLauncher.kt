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
 * then launch in `activity` of external module:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .launcher
 *      .serviceOfWork
 *      .launch(::startService)
 * ```
 *
 * @see Context.startService - e.g.`launch(::startService)`.
 * @see Context.startForegroundService - e.g.`launch(::startForegroundService)`.
 * @see Context.bindService - e.g. use `launch(::xx)` need declare `fun xx(intent: Intent)`
 * method for call bindService.
 * @see ApiLauncher
 */
interface ServiceLauncher : Launcher {

    class Delegate(clazz: Class<*>) {
        private val real by lazy(LazyThreadSafetyMode.NONE) { InternalServiceLauncher(clazz) }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): ServiceLauncher = real
    }

    /**
     * Launch a service for that [Service] class annotated by [ApiLauncher].
     *
     * [launchBlock] is real launch method, that has a created Intent instance
     * as input param, all other fields (action, data, type) are null, though
     * they can be modified later in [onFillIntent].
     */
    fun launchChannel(launchBlock: LaunchServiceBlock): LaunchChannelDelegate
}

/**
 * A block for launch service.
 */
typealias LaunchServiceBlock = (createdIntent: Intent) -> Unit