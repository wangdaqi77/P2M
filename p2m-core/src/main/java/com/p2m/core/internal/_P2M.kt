package com.p2m.core.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.WorkerThread
import com.p2m.core.app.App
import com.p2m.core.channel.InterceptorService
import com.p2m.core.launcher.LaunchActivityChannel
import com.p2m.core.config.P2MConfigManager
import com.p2m.core.internal.channel.ChannelInterceptorContainer
import com.p2m.core.internal.channel.InterceptorServiceDefault
import com.p2m.core.internal.launcher.LaunchActivityHelper
import com.p2m.core.internal.config.InternalP2MConfigManager
import com.p2m.core.internal.execution.Executor
import com.p2m.core.internal.execution.InternalMainExecutor
import com.p2m.core.internal.execution.InternalThreadPoolExecutor
import com.p2m.core.internal.log.logE
import com.p2m.core.internal.log.logW
import com.p2m.core.internal.module.*
import com.p2m.core.internal.module.DefaultModuleFactory
import com.p2m.core.internal.module.DefaultModuleNameCollectorFactory
import com.p2m.core.internal.module.ExternalModuleInfoFinder
import com.p2m.core.internal.module.ManifestModuleInfoFinder
import com.p2m.core.internal.module.ModuleContainerDefault
import com.p2m.core.internal.module.deriver.InternalDriver
import com.p2m.core.module.*
import java.util.concurrent.ExecutorService
import kotlin.collections.ArrayList

@SuppressLint("StaticFieldLeak")
internal object _P2M : ModuleApiProvider, ModuleVisitor {
    internal lateinit var internalContext : Context
    internal val configManager: P2MConfigManager = InternalP2MConfigManager()
    internal val executor : ExecutorService by lazy(LazyThreadSafetyMode.NONE) { InternalThreadPoolExecutor() }
    internal val mainExecutor: Executor by lazy(LazyThreadSafetyMode.NONE) { InternalMainExecutor() }
    internal val interceptorService: InterceptorService by lazy(LazyThreadSafetyMode.NONE) { InterceptorServiceDefault(executor) }
    internal val launchActivityHelper by lazy(LazyThreadSafetyMode.NONE) { LaunchActivityHelper() }
    internal val interceptorContainer = ChannelInterceptorContainer()
    private val moduleContainer = ModuleContainerDefault()
    private lateinit var driver: InternalDriver
    fun init(
        context: Context,
        externalModuleClassLoader: ClassLoader = context.classLoader,
        externalPublicModuleClassName: Array<out String>
    ) {
        check(!_P2M::internalContext.isInitialized) { "`can only be called once." }

        val applicationContext = context.applicationContext
        this.internalContext = applicationContext

        var ideaStartTime = 0L
        val app = App()
            .onEvaluate {
                launchActivityHelper.init(context)
                ideaStartTime = SystemClock.uptimeMillis()

                // ex:
                // Thread.sleep(1000L)
                // log:
                // running `onIdea` too long, it is recommended to shorten to 30 ms.
                // `onIdea` was ran for too long, timeout: 970 ms.
            }
            .onEvaluateTooLongStart {
                logW("running `onIdea` too long, it is recommended to shorten to ${(SystemClock.uptimeMillis() - ideaStartTime).also { ideaStartTime+=it }} ms.")
            }
            .onEvaluateTooLongEnd {
                logW("`onIdea` was ran for too long, timeout: ${SystemClock.uptimeMillis() - ideaStartTime} ms.")
            }

        val externalModules = externalPublicModuleClassName.mapTo(ArrayList(externalPublicModuleClassName.size)) { className ->
                ModuleInfo.fromExternal(
                    classLoader = externalModuleClassLoader,
                    publicClassName = className
                )
            }
        val moduleNameCollector: ModuleNameCollector = DefaultModuleNameCollectorFactory()
            .newInstance("${applicationContext.packageName}.GeneratedModuleNameCollector")
            .apply { collectExternal(externalModules) }
        val moduleInfoFinder: ModuleInfoFinder = GlobalModuleInfoFinder(
            ExternalModuleInfoFinder(externalModules),
            ManifestModuleInfoFinder(applicationContext)
        )
        val moduleFactory: ModuleFactory = DefaultModuleFactory()
        moduleContainer.registerAll(app, moduleNameCollector, moduleInfoFinder, moduleFactory, this)

        this.driver = InternalDriver(applicationContext, app, this.moduleContainer)
        this.driver.considerOpenAwait()
    }

    private fun <MODULE : Module<*>> moduleOf(clazz: Class<out MODULE>): MODULE {
        check(::internalContext.isInitialized) { "Please call `init()` before." }

        val driver = this.driver
        check(driver.isEvaluating?.get() != true) { "Don not call `P2M.moduleOf()` in `onEvaluate()`." }
        driver.safeModuleApiProvider?.get()?.let { moduleProvider ->
            return moduleProvider.moduleOf(clazz)
        }

        val module = moduleContainer.find(clazz)
        check(module != null) { "The ${clazz.moduleName} is not exist for ${clazz.name}" }
        driver.considerOpenAwait()
        @Suppress("UNCHECKED_CAST")
        return module as MODULE
    }

    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(clazz: Class<out Module<MODULE_API>>): MODULE_API =
        moduleOf(clazz).api

    internal fun onLaunchActivityNavigationCompletedBefore(channel: LaunchActivityChannel, intent: Intent) {
        launchActivityHelper.onLaunchActivityNavigationCompletedBefore(channel, intent)
    }

    override fun visit(module: Module<*>) {
        module.initLazy(interceptorContainer)
    }

}