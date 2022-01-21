package com.p2m.example.main.pre_api

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.example.main.R
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForAddAddress
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForBindPhoneNum

@ApiLauncher(
    launcherName = "Mall",
    launchActivityInterceptor = [
        AccountLaunchActivityInterceptorForBindPhoneNum::class,
        AccountLaunchActivityInterceptorForAddAddress::class
    ]
)
class MallActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_mall)

        P2M.apiOf(Account::class.java).event.loginInfo.observe(this, {
            findViewById<TextView>(R.id.main_content).text = """
                手机号：${it?.phone}
                
                收货地址：${it?.address}
            """.trimIndent()
        })
    }
}