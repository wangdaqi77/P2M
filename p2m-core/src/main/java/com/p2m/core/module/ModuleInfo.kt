package com.p2m.core.module

data class ModuleInfo(
    val name: String,
    val publicClass: Class<out Module<*>>,
    val implClass: Class<out Module<*>>,
)