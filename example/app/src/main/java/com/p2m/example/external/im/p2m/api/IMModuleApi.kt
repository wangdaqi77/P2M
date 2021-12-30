package com.p2m.example.external.im.p2m.api

import android.content.Intent
import com.p2m.core.launcher.ActivityLauncher
import com.p2m.core.module.EmptyModuleEvent
import com.p2m.core.module.EmptyModuleService
import com.p2m.core.module.ModuleApi
import com.p2m.core.module.ModuleLauncher


public interface NoneModuleLauncher : ModuleLauncher {
  public val activityOfIM: ActivityLauncher<Intent, Intent>
}


public interface NoneModuleApi : ModuleApi<NoneModuleLauncher, EmptyModuleService, EmptyModuleEvent>
