package com.p2m.core.module

import com.p2m.core.internal.module.ModuleInfo

abstract class ModuleNameCollector {
    internal val moduleNames = mutableSetOf<String>()

    protected fun collect(moduleName: String) {
        moduleNames.add(moduleName)
    }

    internal fun collectExternal(externalModules: ArrayList<ModuleInfo>) {
        for (externalModule in externalModules) {
            moduleNames.add(externalModule.name)
        }
    }
}