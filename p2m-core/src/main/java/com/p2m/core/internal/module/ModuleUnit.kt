package com.p2m.core.internal.module

import com.p2m.core.module.Module
import com.p2m.core.module.ModuleUnit

internal class ModuleUnitImpl(
    val module: Module<*>,
    override val moduleImplClass: Class<out Module<*>>,
    override val modulePublicClass: Class<out Module<*>>
) : ModuleUnit {
    private val dependencies = hashSetOf<Class<out Module<*>>>()

    override fun dependOn(implClass: Class<out Module<*>>) {
        dependencies.add(implClass)
    }

    override fun getDependencies(): Set<Class<out Module<*>>> = dependencies
}