package com.p2m.core.internal.module

import com.p2m.core.exception.P2MException
import com.p2m.core.internal.moduleName
import com.p2m.core.module.*

internal interface SafeModuleApiProvider : ModuleApiProvider {
    var selfAvailable: Boolean
}

@Suppress("UNCHECKED_CAST")
internal class SafeModuleApiProviderImpl(
    private val dependNodes: HashSet<ModuleNode>,
    private val self: Module<*>
) : SafeModuleApiProvider,
    ModuleApiProvider {
    override var selfAvailable: Boolean = false

    override fun <MODULE_API : ModuleApi<*, *, *>> apiOf(clazz: Class<out Module<MODULE_API>>): MODULE_API {
        if (clazz.isInstance(self)) {
            check(selfAvailable) { "${clazz.moduleName} is unavailable in `onEvaluate()` or `onExecuted()` when ${self.internalModuleUnit.moduleName} initializing, only can call `P2M.apiOf(${clazz.simpleName})` in `onExecuted()`" }
            return self.api as MODULE_API
        }

        for (dependNode in dependNodes) {
            if (clazz.isInstance(dependNode.module)) {
                check(dependNode.isExecuted) { "${dependNode.name} is unavailable, that has not been initialized." }
                return dependNode.module.api as MODULE_API
            }
        }
        throw P2MException("${clazz.moduleName} is unavailable, only modules ${self.internalModuleUnit.moduleName} depend on can be obtained when ${self.internalModuleUnit.moduleName} initializing.")
    }
}