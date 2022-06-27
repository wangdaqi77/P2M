package com.p2m.core

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import com.p2m.annotation.module.api.ApiEvent
import com.p2m.core.config.P2MConfigManager
import com.p2m.core.internal._P2M
import com.p2m.core.launcher.GlobalLaunchActivityInterceptor
import com.p2m.core.launcher.LaunchActivityChannel
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.annotation.module.api.ApiService
import com.p2m.core.module.*
import kotlin.reflect.KClass

object P2M : ModuleApiProvider {

    /**
     * Start a config.
     */
    fun config(block: P2MConfigManager.() -> Unit) {
        block(_P2M.configManager)
    }

    /**
     * All module start to initialize.
     *
     * @param context
     * @param externalModuleClassLoader classLoader for [externalPublicModuleClassName]
     * @param externalPublicModuleClassName class name for external public module
     *
     * @see ModuleInit
     */
    @MainThread
    fun init(
        context: Context,
        externalModuleClassLoader: ClassLoader = context.classLoader,
        vararg externalPublicModuleClassName: String
    ) {
        check(Looper.getMainLooper() === Looper.myLooper()) { "`P2M.init()` must be called on the main thread." }
        _P2M.init(context, externalModuleClassLoader, externalPublicModuleClassName)
    }

    /**
     * Get `api` for a module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     * @return a singleton instance of `api`.
     *
     * @see Module
     * @see ModuleApi
     */
    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(clazz: Class<out Module<MODULE_API>>): MODULE_API {
        return _P2M.apiOf(clazz)
    }

    /**
     * Get `launcher` for a module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     * @return a singleton instance of `launcher`.
     *
     * @see Module
     * @see ModuleApi
     * @see ModuleLauncher
     * @see ApiLauncher
     */
    fun <LAUNCHER : ModuleLauncher, MODULE_API : ModuleApi<LAUNCHER, *, *>> launcherOf(clazz: Class<out Module<MODULE_API>>): LAUNCHER {
        return _P2M.apiOf(clazz).launcher
    }

    /**
     * Get `service` for a module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     * @return a singleton instance of `service`.
     *
     * @see Module
     * @see ModuleApi
     * @see ModuleService
     * @see ApiService
     */
    fun <SERVICE : ModuleService, MODULE_API : ModuleApi<*, SERVICE, *>> serviceOf(clazz: Class<out Module<MODULE_API>>): SERVICE {
        return _P2M.apiOf(clazz).service
    }

    /**
     * Get `event` for a module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     * @return a singleton instance of `event`.
     *
     * @see Module
     * @see ModuleApi
     * @see ModuleEvent
     * @see ApiEvent
     */
    fun <EVENT : ModuleEvent, MODULE_API : ModuleApi<*, *, EVENT>> eventOf(clazz: Class<out Module<MODULE_API>>): EVENT {
        return _P2M.apiOf(clazz).event
    }

    /**
     * Add the interceptor to head for every `ActivityLauncher` instance.
     *
     * Run the following code, interceptors execution order on runtime:
     * `head2 -> head1 -> annotatedInterceptors -> tail`:
     * ```
     * P2M.addInterceptorToHead(context, head1)
     * P2M.addInterceptorToHead(context, head2)
     * ```
     *
     * @see ApiLauncher add `annotatedInterceptorClasses` for a `activityLauncher`.
     * @see LaunchActivityChannel.addAnnotatedInterceptorBefore Add the annotated interceptor
     * before this `activityLauncher.annotatedInterceptorClasses`.
     * @see LaunchActivityChannel.addAnnotatedInterceptorAfter Add the annotated interceptor
     * after this `activityLauncher.annotatedInterceptorClasses`.
     */
    fun addInterceptorToHead(context: Context, launchActivityInterceptor: GlobalLaunchActivityInterceptor) {
        _P2M.interceptorContainer.registerGlobalHead(context.applicationContext, launchActivityInterceptor)
    }

    /**
     * Add the interceptor to tail for every `ActivityLauncher` instance.
     *
     * Run the following code, interceptors execution order on runtime:
     * `head -> annotatedInterceptors -> tail1 -> tail2`:
     * ```
     * P2M.addInterceptorToTail(context, tail1)
     * P2M.addInterceptorToTail(context, tail2)
     * ```
     *
     * @see ApiLauncher add `annotatedInterceptorClasses` for a `activityLauncher`.
     * @see LaunchActivityChannel.addAnnotatedInterceptorBefore Add the annotated interceptor
     * before this `activityLauncher.annotatedInterceptorClasses`.
     * @see LaunchActivityChannel.addAnnotatedInterceptorAfter Add the annotated interceptor
     * after this `activityLauncher.annotatedInterceptorClasses`.
     */
    fun addInterceptorToTail(context: Context, launchActivityInterceptor: GlobalLaunchActivityInterceptor) {
        _P2M.interceptorContainer.registerGlobalTail(context.applicationContext, launchActivityInterceptor)
    }

    /**
     * Remove the `launchActivityInterceptor` for every `ActivityLauncher` instance.
     */
    fun removeInterceptor(launchActivityInterceptor: GlobalLaunchActivityInterceptor) {
        _P2M.interceptorContainer.unregisterGlobal(launchActivityInterceptor)
    }
}

inline val <MODULE_API : ModuleApi<*, *, *>> Class<out Module<MODULE_API>>.api: MODULE_API
    get() = P2M.apiOf(this)

inline val <LAUNCHER : ModuleLauncher, MODULE_API : ModuleApi<LAUNCHER, *, *>> Class<out Module<MODULE_API>>.launcher: LAUNCHER
    get() = P2M.launcherOf(this)

inline val <SERVICE : ModuleService, MODULE_API : ModuleApi<*, SERVICE, *>> Class<out Module<MODULE_API>>.service: SERVICE
    get() = P2M.serviceOf(this)

inline val <EVENT : ModuleEvent, MODULE_API : ModuleApi<*, *, EVENT>> Class<out Module<MODULE_API>>.event: EVENT
    get() = P2M.eventOf(this)

inline val <MODULE_API : ModuleApi<*, *, *>> KClass<out Module<MODULE_API>>.api: MODULE_API
    get() = P2M.apiOf(this.java)

inline val <LAUNCHER : ModuleLauncher, MODULE_API : ModuleApi<LAUNCHER, *, *>> KClass<out Module<MODULE_API>>.launcher: LAUNCHER
    get() = P2M.launcherOf(this.java)

inline val <SERVICE : ModuleService, MODULE_API : ModuleApi<*, SERVICE, *>> KClass<out Module<MODULE_API>>.service: SERVICE
    get() = P2M.serviceOf(this.java)

inline val <EVENT : ModuleEvent, MODULE_API : ModuleApi<*, *, EVENT>> KClass<out Module<MODULE_API>>.event: EVENT
    get() = P2M.eventOf(this.java)
