package com.p2m.core.module

abstract class ModuleNameCollector {
    internal val moduleNames = mutableSetOf<String>()

    protected fun collect(moduleName: String) {
        moduleNames.add(moduleName)
    }

    internal fun collectExternal(externalModules: Array<ModuleInfo>) {
        for (externalModule in externalModules) {
            moduleNames.add(externalModule.name)
        }
    }
}