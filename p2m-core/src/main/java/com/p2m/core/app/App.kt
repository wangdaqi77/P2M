package com.p2m.core.app

import com.p2m.core.internal.app.AppModuleInit
import com.p2m.core.module.*

class App : Module<AppModuleApi>() {
    override val init: ModuleInit = AppModuleInit()
    override val api: AppModuleApi = AppModuleApi()
    override val publicClass: Class<out Module<*>> = App::class.java
}