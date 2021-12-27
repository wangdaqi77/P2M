package com.p2m.core.internal.module

import com.p2m.core.module.ModuleNameCollector
import com.p2m.core.module.ModuleNameCollectorFactory

internal class DefaultModuleNameCollectorFactory : ModuleNameCollectorFactory {
    override fun newInstance(clazzName: String): ModuleNameCollector {
        return Class.forName(clazzName).newInstance() as ModuleNameCollector
    }
}