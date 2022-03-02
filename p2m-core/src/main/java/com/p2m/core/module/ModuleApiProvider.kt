package com.p2m.core.module

interface ModuleApiProvider {

    /**
     * Get a module api of [clazz].
     *
     * @param clazz its class name is defined module name in `settings.gradle`.
     */
    fun <MODULE_API : ModuleApi<*, *, *>> apiOf(clazz: Class<out Module<MODULE_API>>): MODULE_API

}