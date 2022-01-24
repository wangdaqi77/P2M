package com.p2m.example.mall

import android.content.Context
import com.p2m.example.mall.pre_api.MallUserInfo

class MallDiskCache(private val context: Context) {

    // 保存登录用户信息
    fun saveMallUserInfo(info: MallUserInfo?) {
        val sp = context.getSharedPreferences("mall_user", Context.MODE_PRIVATE)
        sp.edit().putString("user_address", info?.address).apply()
    }

    // 读取登录用户信息
    fun readMallUserInfo(): MallUserInfo? {
        val sp = context.getSharedPreferences("mall_user", Context.MODE_PRIVATE)
        val address = sp.getString("user_address", null)
        return MallUserInfo().apply {
            this.address = address
        }
    }

    // 清空数据
    fun clear() {
        saveMallUserInfo(null)
    }
}
