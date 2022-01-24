package com.p2m.example.mall.pre_api

import android.content.Context
import com.p2m.annotation.module.api.ApiService
import com.p2m.core.P2M
import com.p2m.example.mall.MallDiskCache
import com.p2m.example.mall.p2m.api.Mall
import com.p2m.example.mall.p2m.impl.mutable

@ApiService
class MallService {

    /**
     * 删除收货地址并更新缓存，之后将会发送一个事件
     */
    fun deleteAddress(context: Context) {
        P2M.apiOf(Mall::class.java)
            .event
            .mutable()
            .mallUserInfo
            .run {
                getValue()?.apply {
                        address = null
                        MallDiskCache(context).saveMallUserInfo(this)
                        postValue(this)
                }
            }
    }
}