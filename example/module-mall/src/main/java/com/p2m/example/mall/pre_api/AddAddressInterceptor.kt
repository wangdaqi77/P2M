package com.p2m.example.mall.pre_api

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.p2m.annotation.module.api.ApiLaunchActivityInterceptor
import com.p2m.core.P2M
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.launcher.LaunchActivityInterceptorCallback
import com.p2m.example.mall.p2m.api.Mall

/**
 * 添加收货地址拦截器
 *
 * 如果未添加则跳转到添加收货地址Activity
 */
@ApiLaunchActivityInterceptor("AddAddress")
class AddAddressInterceptor : ILaunchActivityInterceptor {
    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun process(callback: LaunchActivityInterceptorCallback) {
        try {
            val mall = P2M.apiOf(Mall::class.java)
            val address = mall.event.mallUserInfo.getValue()?.address
            val noAdd = address.isNullOrEmpty()
            if (noAdd) {
                // 未添加，重定向到添加收货地址界面
                callback.onRedirect(
                    redirectChannel = mall.launcher.activityOfAddAddress
                        .launchChannel { intent ->
                            if (context !is Activity) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                )
            } else {
                // 添加过，继续
                callback.onContinue()
            }
        } catch (e: Throwable) {
            // 异常，中断
            callback.onInterrupt(e)
        }
    }
}