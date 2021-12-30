package com.p2m.core.module

import com.p2m.core.exception.P2MException
import com.p2m.core.internal.module.ModuleInfo

abstract class ModuleNameCollector {
    internal val moduleNames = mutableSetOf<String>()

    protected fun collect(moduleName: String) {
        moduleNames.add(moduleName)
    }

    internal fun collectExternal(externalModules: ArrayList<ModuleInfo>) {
        for (externalModule in externalModules) {
            if (!moduleNames.add(externalModule.name)) {
                throw P2MException("The same module name exists: ${externalModule.name}, external public module class: ${externalModule.publicClass}")
            }
        }
    }
}