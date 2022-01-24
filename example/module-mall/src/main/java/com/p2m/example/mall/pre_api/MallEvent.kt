package com.p2m.example.mall.pre_api

import com.p2m.annotation.module.api.ApiEvent
import com.p2m.annotation.module.api.ApiEventField

@ApiEvent
interface MallEvent {

    /**
     * 用户商城信息
     */
    @ApiEventField
    val mallUserInfo: MallUserInfo?
}