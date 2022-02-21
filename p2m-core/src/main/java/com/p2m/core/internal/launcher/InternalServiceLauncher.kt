package com.p2m.core.internal.launcher

import android.content.Context
import com.p2m.core.launcher.LaunchServiceBlock
import com.p2m.core.launcher.LaunchServiceChannel
import com.p2m.core.launcher.ServiceLauncher

internal class InternalServiceLauncher(private val clazz: Class<*>) : ServiceLauncher {
    override fun launchChannel(launchBlock: LaunchServiceBlock) =
        LaunchServiceChannel.create(this) {
            InternalSafeIntent(clazz).apply(launchBlock)
        }

    override fun stop(context: Context) {
        context.stopService(InternalSafeIntent(clazz))
    }
}