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
     * 清除用户信息并启动登录界面
     */
    fun logout(context: Context){

        P2M.apiOf(Account::class.java).run {
            // 清除用户缓存
            event.mutable().apply {
                UserDiskCache(context).clear()
                loginState.postValue(false)
                loginInfo.postValue(null)
            }

            // 跳转到登录界面
            launcher
                .activityOfLogin
                .launchChannel { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                .navigation()
        }
    }


    /**
     * 解绑手机号并更新缓存，之后将会发送一个事件
     */
    fun unbindPhone(context: Context){
        P2M.apiOf(Account::class.java)
            .event
            .mutable()
            .loginInfo
            .run {
                getValue()
                    ?.apply {
                        phone = null
                        UserDiskCache(context).saveLoginUserInfo(this)
                        postValue(this)
                    }
            }
    }

}
