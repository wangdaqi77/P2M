package com.p2m.example.account.pre_api

import com.p2m.annotation.module.api.ApiUse

@ApiUse
class LoginUserInfo {
    var userId: String? = null
    var userName: String? = null
    var phone: String? = null
}

