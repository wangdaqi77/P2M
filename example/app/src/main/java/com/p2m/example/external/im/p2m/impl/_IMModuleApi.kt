// Automatically generated file by P2M. DO NOT MODIFY
package com.p2m.example.external.im.p2m.impl

import android.content.Intent
import com.p2m.core.launcher.ActivityLauncher
import com.p2m.core.launcher.DefaultActivityResultContractCompat
import com.p2m.core.module.EmptyModuleEvent
import com.p2m.core.module.EmptyModuleService
import com.p2m.example.external.im.IMActivity
import com.p2m.example.external.im.p2m.api.IMModuleApi
import com.p2m.example.external.im.p2m.api.IMModuleLauncher

public class _IMModuleLauncher : IMModuleLauncher {

  public override val activityOfIM: ActivityLauncher<Unit, Intent> by
      ActivityLauncher.Delegate(IMActivity::class.java) { DefaultActivityResultContractCompat() }
}

public class _IMModuleApi : IMModuleApi {
  public override val launcher: IMModuleLauncher by lazy() {
      _IMModuleLauncher() }

  public override val service: EmptyModuleService by lazy() { com.p2m.core.module.EmptyModuleService
      }

  public override val event: EmptyModuleEvent by lazy() { com.p2m.core.module.EmptyModuleEvent }
}
