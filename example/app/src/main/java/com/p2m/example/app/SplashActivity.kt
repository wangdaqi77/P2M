package com.p2m.example.app

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.p2m.core.P2M
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForLogin
import com.p2m.example.main.p2m.api.Main


class SplashActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        findViewById<View>(R.id.fullscreen_content).postDelayed( {
            jumpMain1()  // 使用拦截器
            // jumpMain2()  // 不使用拦截器
        }, 2000)
        return
    }

    private fun jumpMain1() {
        P2M.apiOf(Main::class.java)
            .launcher
            .activityOfMain
            .launchChannel {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
            .addAnnotatedInterceptorBefore(AccountLaunchActivityInterceptorForLogin::class) // 首先登录
            //.redirectionMode(ChannelRedirectionMode.FLEXIBLY) // 默认就是ChannelRedirectionMode.FLEXIBLY
            .navigation()
        finish()
    }

    private fun jumpMain2() {
        // 获取登录状态
        val loginState = P2M.apiOf(Account::class.java).event.loginState.getValue()
        if (loginState == true) {
            // 登录过
            P2M.apiOf(Main::class.java)
                .launcher
                .activityOfMain
                .launchChannel {
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
                .navigation()
            finish()
        } else {
            // 未登录
            P2M.apiOf(Account::class.java)
                .launcher
                .activityOfLogin
                .launchChannel {
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
                .navigation()
            finish()
        }
    }

}