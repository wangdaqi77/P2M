package com.p2m.core.module

import com.p2m.core.internal.module.ModuleUnitImpl
import com.p2m.core.internal.module.ModuleVisitor

/**
 * A module has a [Module].
 *
 * The [Module] public sub class is auto generated by P2M-APT, it's class name is defined module name
 * in settings.gradle.
 *
 * @see ModuleApi  - a module api.
 * @see ModuleInit - a module initialization.
 */
abstract class Module<MODULE_API : ModuleApi<*, *, *>> {
    abstract val api: MODULE_API
    protected abstract val init: ModuleInit
    @Suppress("UNCHECKED_CAST")
    protected open val publicClass: Class<out Module<*>> = this.javaClass.superclass as Class<out Module<*>>
    internal val internalInit: ModuleInit
        get() = init
    @Suppress("LeakingThis")
    internal val internalModuleUnit by lazy(LazyThreadSafetyMode.NONE) {
        ModuleUnitImpl(this, this.javaClass, publicClass)
    }

    protected fun dependOn(moduleClass: Class<out Module<*>>, implClassName: String) {
        internalModuleUnit.dependOn(moduleClass, implClassName)
    }

    internal fun accept(visitor: ModuleVisitor){
        visitor.visit(this)
    }
}