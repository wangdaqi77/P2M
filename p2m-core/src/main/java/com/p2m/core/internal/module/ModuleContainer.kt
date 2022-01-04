package com.p2m.core.internal.module

import com.p2m.core.module.Module
import com.p2m.core.module.ModuleFactory
import com.p2m.core.module.ModuleNameCollector

internal interface ModuleContainer{
    /**
     * Found a module in the container.
     *
     * @param clazz  apiClass or implClass.
     */
    fun find(clazz: Class<out Module<*>>): Module<*>?

    fun getAll(): Collection<Module<*>>
}


internal class ModuleContainerDefault() : ModuleRegister, ModuleContainer {
    // K:impl V:ModuleUnitImpl
    private val container = HashMap<Class<out Module<*>>, Module<*>>()
    // K:public api V:impl
    private val clazzMap = HashMap<Class<out Module<*>>, Class<out Module<*>>>()

    internal fun registerAll(
        topModule: Module<*>,
        moduleNameCollector: ModuleNameCollector,
        moduleInfoFinder: ModuleInfoFinder,
        moduleFactory: ModuleFactory,
        vararg visitor: ModuleVisitor
    ) {
        register(topModule)

        for (moduleName in moduleNameCollector.moduleNames) {
            loopCreateModule(moduleName, moduleNameCollector, moduleInfoFinder, moduleFactory) { module ->
                register(module)
                topModule.internalModuleUnit.dependOn(module.internalModuleUnit.moduleImplClass)
            }
        }

        container.values.forEach{ module ->
            visitor.forEach {  visitor ->
                visitor.visit(module)
            }
        }
    }

    private fun loopCreateModule(
        moduleName: String,
        moduleNameCollector: ModuleNameCollector,
        moduleInfoFinder: ModuleInfoFinder,
        moduleFactory: ModuleFactory,
        onCreatedModule: (Module<*>) -> Unit
    ) {
        moduleInfoFinder.findModuleInfo(moduleName)
            ?.let { moduleInfo ->
                val implClass = moduleInfo.implClass
                container[implClass] ?: moduleFactory.newInstance(moduleInfo.implClass)
            }
            ?.also(onCreatedModule)
            ?.also { module ->
                for (dependency in module.dependencies) {
                    loopCreateModule(dependency, moduleNameCollector, moduleInfoFinder, moduleFactory) { dependencyModule->
                        onCreatedModule(dependencyModule)
                        module.internalModuleUnit.dependOn(dependencyModule.internalModuleUnit.moduleImplClass)
                    }
                }
            }
    }

    override fun register(module: Module<*>) {
        val moduleUnit = module.internalModuleUnit
        if (container.containsKey(moduleUnit.moduleImplClass)) return
        clazzMap[moduleUnit.modulePublicClass] = moduleUnit.moduleImplClass
        container[moduleUnit.moduleImplClass] = module
    }

    override fun find(clazz: Class<out Module<*>>): Module<*>? = container[clazzMap[clazz]] ?: container[clazz]

    override fun getAll(): Collection<Module<*>> = container.values

}