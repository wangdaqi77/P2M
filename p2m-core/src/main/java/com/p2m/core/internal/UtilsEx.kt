package com.p2m.core.internal

import com.p2m.core.module.Module

internal inline val Class<out Module<*>>.moduleName: String
    get() = simpleName.removePrefix("_")

internal val NULL = Symbol("NULL")