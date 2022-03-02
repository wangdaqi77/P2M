package com.p2m.core.internal.module

import com.p2m.core.exception.P2MException
import com.p2m.core.internal.moduleName
import com.p2m.core.module.*

internal interface SafeModuleApiProvider : ModuleApiProvider {
    var selfAvailable: Boolean

    /**
     * Get a module.
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     */
    fun <MODULE: Module<*>> moduleOf(clazz: Class<out MODULE>): MODULE
}

@Suppress("UNCHECKED_CAST")
internal class SafeModuleApiProviderImpl(
    private val dependNodes: HashSet<ModuleNode>,
    private val self: Module<*>
) : SafeModuleApiProvider,
    ModuleApiProvider {
    override var selfAvailable: Boolean = false

    override fun <MODULE : Module<*>> moduleOf(clazz: Class<out MODULE>): MODULE {
        if (clazz.isInstance(self)) {
            check(selfAvailable) { "${clazz.moduleName} is unavailable in `onEvaluate()` or `onCompleted()` when ${self.internalModuleUnit.moduleName} initializing, only can call `P2M.apiOf(${clazz.simpleName})` in `onCompleted()`" }
            return self as MODULE
        }

        for (dependNode in dependNodes) {
            if (clazz.isInstance(dependNode.module)) {
                check(dependNode.isCompleted) { "${dependNode.name} is unavailable, that has not been initialized." }
                return dependNode.module as MODULE
            }
        }
        throw P2MException("${clazz.moduleName} is unavailable, only modules ${self.internalModuleUnit.moduleName} depend on can be obtained when ${self.internalModuleUnit.moduleName} initializing.")
    }

    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(clazz: Class<out Module<MODULE_API>>): MODULE_API =
        moduleOf(clazz).api
}