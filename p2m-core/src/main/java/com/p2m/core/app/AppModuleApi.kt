package com.p2m.core.app

import com.p2m.core.internal.app.AppModuleService
import com.p2m.core.module.EmptyModuleEvent
import com.p2m.core.module.EmptyModuleLauncher
import com.p2m.core.module.ModuleApi

class AppModuleApi(
    override val launcher: EmptyModuleLauncher = EmptyModuleLauncher,
    override val service: AppModuleService = AppModuleService(),
    override val event: EmptyModuleEvent = EmptyModuleEvent
) : ModuleApi<EmptyModuleLauncher, AppModuleService, EmptyModuleEvent>