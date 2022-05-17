package com.p2m.example.appb

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.p2m.core.P2M
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForLogin
import com.p2m.example.main.p2m.api.Main


class SplashBActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<View>(R.id.fullscreen_content).postDelayed( {
            P2M.apiOf(Main::class.java)
                .launcher
                .activityOfMain
                .launchChannel {
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
                .addInterceptorBefore(AccountLaunchActivityInterceptorForLogin::class) // 首先登录
                //.redirectionMode(ChannelRedirectionMode.FLEXIBLY) // 默认就是ChannelRedirectionMode.FLEXIBLY
                .navigation()
            finish()
        }, 2000)
    }

}