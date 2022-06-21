package com.p2m.example.mall.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.p2m.core.P2M
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForLogin
import com.p2m.example.mall.p2m.api.Mall

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))
        P2M.apiOf(Mall::class.java)
            .launcher
            .activityOfMall
            .launchChannel { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addAnnotatedInterceptorBefore(AccountLaunchActivityInterceptorForLogin::class) // 首先登录
//                .redirectionMode(ChannelRedirectionMode.FLEXIBLY)
            .navigation()
        finish()
    }
}