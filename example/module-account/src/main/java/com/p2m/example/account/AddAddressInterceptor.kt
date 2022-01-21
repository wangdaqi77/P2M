package com.p2m.example.account

import android.content.Context
import com.p2m.annotation.module.api.LaunchActivityInterceptor
import com.p2m.core.P2M
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.launcher.LaunchActivityInterceptorCallback
import com.p2m.example.account.p2m.api.Account

/**
 * 添加收货地址拦截器
 *
 * 如果未添加则跳转到添加收货地址Activity
 */
@LaunchActivityInterceptor("AddAddress")
class AddAddressInterceptor : ILaunchActivityInterceptor {
    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun process(callback: LaunchActivityInterceptorCallback) {
        try {
            val account = P2M.apiOf(Account::class.java)
            val address = account.event.loginInfo.getValue()?.address
            val noAdd = address.isNullOrEmpty()
            if (noAdd) {
                // 未添加
                callback.onRedirect(
                    redirectChannel = account.launcher
                        .activityOfAddAddress
                        .launchChannel {
                            context.startActivity(it)
                        }
                )
            } else {
                // 添加过
                callback.onContinue()
            }
        } catch (e: Throwable) {
            // 中断
            callback.onInterrupt(e)
        }
    }
}