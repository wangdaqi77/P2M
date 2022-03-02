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
 * 添加绑定手机号拦截器
 *
 * 如果未绑定手机号则跳转到绑定手机号Activity
 */
@ApiLaunchActivityInterceptor(interceptorName = "BindPhoneNum")
class BindPhoneInterceptor : ILaunchActivityInterceptor {
    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun process(callback: LaunchActivityInterceptorCallback) {
        try {
            val account = P2M.apiOf(Account::class.java)
            val phone = account.event.loginInfo.getValue()?.phone
            val unbind = phone.isNullOrEmpty()
            if (unbind) {
                // 未绑定，重定向到绑定界面
                callback.onRedirect(
                    redirectChannel = account.launcher.activityOfBindPhone
                        .launchChannel { intent ->
                            if (context !is Activity) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                )
            } else {
                // 绑定过，继续
                callback.onContinue()
            }

        } catch (e: Throwable) {
            // 异常，中断
            callback.onInterrupt(e)
        }
    }
}