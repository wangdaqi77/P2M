package com.p2m.example.external.im.p2m.api

import android.content.Intent
import com.p2m.core.launcher.ActivityLauncher
import com.p2m.core.module.EmptyModuleEvent
import com.p2m.core.module.EmptyModuleService
import com.p2m.core.module.ModuleApi
import com.p2m.core.module.ModuleLauncher


public interface IMModuleLauncher : ModuleLauncher {
  public val activityOfIM: ActivityLauncher<Unit, Intent>
}


public interface IMModuleApi : ModuleApi<IMModuleLauncher, EmptyModuleService, EmptyModuleEvent>
