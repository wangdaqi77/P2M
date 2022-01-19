package com.p2m.example.account

import android.content.Context
import com.p2m.annotation.module.api.LaunchActivityInterceptor
import com.p2m.core.P2M
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.launcher.LaunchActivityInterceptorCallback
import com.p2m.example.account.p2m.api.Account

/**
 * 添加绑定手机号拦截器
 *
 * 如果未绑定手机号则跳转到绑定手机号Activity
 * */
@LaunchActivityInterceptor("BindPhoneNum")
class BindPhoneInterceptor : ILaunchActivityInterceptor {
    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun process(callback: LaunchActivityInterceptorCallback) {
        try {
            val account = P2M.apiOf(Account::class.java)
            val phone = account.event.loginInfo.getValue()?.phone
            if (phone.isNullOrEmpty()) {
                callback.onRedirect(
                    redirectChannel = account.launcher
                        .activityOfBindPhone
                        .launchChannel {
                            context.startActivity(it)
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