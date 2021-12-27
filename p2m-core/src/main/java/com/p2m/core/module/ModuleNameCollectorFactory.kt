package com.p2m.core.module

interface ModuleNameCollectorFactory {
    fun newInstance(clazzName: String): ModuleNameCollector
}