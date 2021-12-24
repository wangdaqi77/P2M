package com.p2m.example.account.pre_api

import android.content.Context
import android.content.Intent
import com.p2m.core.P2M
import com.p2m.annotation.module.api.*
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.impl.mutable

@ApiService
class AccountService{
    /**
     * 退出登录
     */
    fun logout(context: Context){
        // 清除用户缓存
        P2M.apiOf(Account::class.java)
            .event
            .mutable()
            .apply {
                UserDiskCache(context).clear()

                loginState.setValue(false)
                loginInfo.setValue(null)
            }

        // 跳转到登录界面
        P2M.apiOf(Account::class.java)
            .launcher
            .activityOfLogin
            .launchChannel {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
            .navigation()
    }
}
