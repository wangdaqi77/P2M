package com.p2m.core.internal.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.p2m.core.channel.InterceptorServiceDefault
import com.p2m.core.channel.RecoverableChannel
import com.p2m.core.internal.channel.RecoverableChannelHelper
import com.p2m.core.module.ModuleService

class AppModuleService internal constructor(): ModuleService {
    internal val interceptorService = InterceptorServiceDefault()
    private var isInitialized = false

    internal fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        RecoverableChannelHelper.init(context)
    }

    internal fun saveRecoverableChannel(intent: Intent, recoverableChannel: RecoverableChannel){
        RecoverableChannelHelper.saveRecoverableChannel(intent, recoverableChannel)
    }

    fun findRecoverableChannel(activity: Activity): RecoverableChannel? {
        return RecoverableChannelHelper.findRecoverableChannel(activity)
    }

}
