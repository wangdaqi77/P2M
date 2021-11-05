package com.p2m.core.internal.module

import com.p2m.core.module.*

@Suppress("UNCHECKED_CAST")
internal class SafeModuleApiProviderImpl(
    private val moduleContainer: ModuleContainerImpl,
    private val self: Module<*>
) :
    SafeModuleApiProvider,
    ModuleApiProvider {

    private fun <MODULE : Module<*>> checkSelf(clazz: Class<MODULE>): Boolean {
        return clazz.isInstance(self)
    }

    private fun <MODULE_API : ModuleApi<*, *, *>, MODULE : Module<MODULE_API>> getSelf(
        clazz: Class<MODULE>
    ): MODULE_API {
        check (checkSelf(clazz)) { "Please Call get(${self.internalModuleUnit.modulePublicClass.simpleName}::class.java)." }
        return self.api as MODULE_API
    }

    override fun <MODULE_API : ModuleApi<*, *, *>, MODULE : Module<MODULE_API>> moduleApiOf(
        clazz: Class<MODULE>
    ): MODULE_API {
        if (checkSelf(clazz)) {
            return getSelf(clazz)
        }
        return moduleContainer.find(clazz)?.api as MODULE_API
    }
}