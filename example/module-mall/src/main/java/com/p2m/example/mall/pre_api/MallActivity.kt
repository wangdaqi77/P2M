package com.p2m.example.mall.pre_api

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.example.mall.R
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForBindPhoneNum
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForLogin
import com.p2m.example.mall.p2m.api.Mall

@ApiLauncher(
    launcherName = "Mall",
    launchActivityInterceptor = [
        AccountLaunchActivityInterceptorForBindPhoneNum::class,
        AddAddressInterceptor::class
    ]
)
class MallActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mall_activity_mall)

        P2M.apiOf(Account::class.java).event.loginInfo.observe(this, {
            findViewById<TextView>(R.id.mall_tv_phone).text = "手机号：${it?.phone ?: "未绑定"}"
        })
        P2M.apiOf(Mall::class.java).event.mallUserInfo.observe(this, {
            findViewById<TextView>(R.id.mall_tv_address).text = "收货地址：${it?.address ?: "未添加"}"
        })
    }
}