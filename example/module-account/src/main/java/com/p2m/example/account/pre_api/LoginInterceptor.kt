package com.p2m.example.account.pre_api

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.p2m.annotation.module.api.ApiLaunchActivityInterceptor
import com.p2m.core.P2M
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.launcher.LaunchActivityInterceptorCallback
import com.p2m.example.account.p2m.api.Account

/**
 * 登录拦截器
 *
 * 如果未登录则会重定向到登录界面
 */
@ApiLaunchActivityInterceptor("Login")
class LoginInterceptor : ILaunchActivityInterceptor {
    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun process(callback: LaunchActivityInterceptorCallback) {
        try {
            val account = P2M.apiOf(Account::class.java)
            val isLogin = account.event.loginState.getValue() ?: false
            if (!isLogin) {
                callback.onRedirect(
                    redirectChannel = account.launcher.activityOfLogin
                        .launchChannel { intent ->
                            if (context !is Activity) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                )
            } else {
                callback.onContinue()
            }
        } catch (e: Throwable) {
            callback.onInterrupt(e)
        }
    }
}