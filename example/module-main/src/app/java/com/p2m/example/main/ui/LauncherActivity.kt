package com.p2m.example.main.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.p2m.core.P2M
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.main.p2m.api.Main

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))

        // 获取登录状态
        val loginState = P2M.apiOf(Account::class.java)
            .event
            .loginState
            .getValue()

        if (loginState == true) {
            // 登录过
            P2M.apiOf(Main::class.java)
                .launcher
                .activityOfMain
                .launch(this) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            finish()
        }else{
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