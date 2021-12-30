package com.p2m.core.internal.app

import android.content.Context
import com.p2m.core.module.ModuleService

class AppModuleService internal constructor(): ModuleService {
    private var isInitialized = false

    internal fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
    }
}
