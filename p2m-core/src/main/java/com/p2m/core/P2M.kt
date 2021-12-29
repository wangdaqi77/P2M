package com.p2m.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import com.p2m.core.app.App
import com.p2m.core.config.P2MConfigManager
import com.p2m.core.internal.config.InternalP2MConfigManager
import com.p2m.core.internal.execution.InternalExecutor
import com.p2m.core.internal.module.*
import com.p2m.core.internal.module.DefaultModuleFactory
import com.p2m.core.internal.module.DefaultModuleNameCollectorFactory
import com.p2m.core.internal.module.ExternalModuleInfoFinder
import com.p2m.core.internal.module.ManifestModuleInfoFinder
import com.p2m.core.internal.module.ModuleContainerDefault
import com.p2m.core.internal.moduleName
import com.p2m.core.internal.module.deriver.InternalDriver
import com.p2m.core.module.*
import java.util.*

@SuppressLint("StaticFieldLeak")
object P2M : ModuleApiProvider{
    internal lateinit var internalContext : Context
    internal val _executor by lazy { InternalExecutor() }
    internal val configManager: P2MConfigManager = InternalP2MConfigManager()
    private val moduleContainer = ModuleContainerDefault()
    private lateinit var driver: InternalDriver

    /**
     * Start a config.
     */
    fun config(block: P2MConfigManager.() -> Unit) {
        block(this.configManager)
    }

    /**
     * Initialization.
     */
    @MainThread
    fun init(context: Context, vararg externalModule: ModuleInfo) {
        check(!this::internalContext.isInitialized) { "`P2M.init()` can only be called once." }
        check(Looper.getMainLooper() === Looper.myLooper()) { "`P2M.init()` must be called on the main thread." }

        val applicationContext = context.applicationContext
        this.internalContext = applicationContext


        val externalModules = Arrays.copyOf(externalModule, externalModule.size)
        val moduleNameCollector: ModuleNameCollector = DefaultModuleNameCollectorFactory()
            .newInstance("${applicationContext.packageName}.GeneratedModuleNameCollector")
            .apply { collectExternal(externalModules) }
        val moduleInfoFinder: ModuleInfoFinder = GlobalModuleInfoFinder(
            ExternalModuleInfoFinder(externalModules),
            ManifestModuleInfoFinder(applicationContext)
        )
        val moduleFactory: ModuleFactory = DefaultModuleFactory()

        val app = App()
        moduleContainer.register(app, moduleNameCollector, moduleInfoFinder, moduleFactory)

        this.driver = InternalDriver(applicationContext, app, this.moduleContainer)
        this.driver.considerOpenAwait()
    }

    /**
     * Get instance of `api` by [clazz] of module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     *
     * @see Module
     * @see ModuleApi
     */
    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(
        clazz: Class<out Module<MODULE_API>>
    ): MODULE_API {
        check(::internalContext.isInitialized) { "Must call P2M.init() before when call here." }

        val driver = this.driver
        check(driver.isEvaluating?.get() != true) { "Don not call `P2M.apiOf()` in `onEvaluate()`." }
        driver.safeModuleApiProvider?.get()?.let { moduleProvider ->
            return moduleProvider.apiOf(clazz)
        }

        val module = moduleContainer.find(clazz)
        check(module != null) { "The ${clazz.moduleName} is not exist for ${clazz.name}" }
        driver.considerOpenAwait()
        @Suppress("UNCHECKED_CAST")
        return module.api as MODULE_API
    }
}