package com.p2m.example.account

import android.content.Context
import com.p2m.example.account.pre_api.LoginUserInfo

class UserDiskCache(private val context: Context) {

    // 保存登录状态
    fun saveLoginState(b: Boolean) {
        val sp = context.getSharedPreferences("login_user", Context.MODE_PRIVATE)
        sp.edit().putBoolean("login_state", b).apply()
    }

    // 读取登录状态
    fun readLoginState():Boolean {
        val sp = context.getSharedPreferences("login_user", Context.MODE_PRIVATE)
        return sp.getBoolean("login_state", false)
    }

    // 保存登录用户信息
    fun saveLoginUserInfo(info: LoginUserInfo?) {
        val sp = context.getSharedPreferences("login_user", Context.MODE_PRIVATE)
        sp.edit().putString("login_user_name", info?.userName).apply()
        sp.edit().putString("login_user_id", info?.userId).apply()
        sp.edit().putString("login_user_phone", info?.phone).apply()
    }

    // 读取登录用户信息
    fun readLoginUserInfo(): LoginUserInfo? {
        val sp = context.getSharedPreferences("login_user", Context.MODE_PRIVATE)
        val userName = sp.getString("login_user_name", null)
        val userId = sp.getString("login_user_id", null)
        val phone = sp.getString("login_user_phone", null)
        return if (userId != null) {
            LoginUserInfo().apply {
                this.userId = userId
                this.userName = userName
                this.phone = phone
            }
        }else {
            null
        }
    }

    // 清空数据
    fun clear() {
        saveLoginState(false)
        saveLoginUserInfo(null)
    }
}