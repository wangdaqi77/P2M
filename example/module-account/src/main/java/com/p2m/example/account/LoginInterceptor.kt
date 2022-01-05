package com.p2m.example.account

import android.content.Context
import com.p2m.annotation.module.api.ApiLauncherInterceptor
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.launcher.LaunchActivityInterceptorCallback

@ApiLauncherInterceptor
class LoginInterceptor : ILaunchActivityInterceptor {
    override fun init(context: Context) {

    }

    override fun process(callback: LaunchActivityInterceptorCallback) {
        callback.onContinue()
    }
}