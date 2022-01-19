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
import com.p2m.core.channel.ChannelRedirectionMode
import com.p2m.example.main.R
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.main.p2m.api.Main

@ApiLauncher("Main")
class MainActivity : AppCompatActivity() {

    private val modifyAccountNameLauncherForActivityResult =
        P2M.apiOf(Account::class.java)
            .launcher
            .activityOfModifyAccountName
            .registerResultLauncher(this) { resultCode, output ->
                when (resultCode) {
                    RESULT_OK -> Toast.makeText(this, "修改成功，output: $output", Toast.LENGTH_SHORT).show()
                    else -> {
                    }
                }
            }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity_main)

        // 监听用户信息事件
        P2M.apiOf(Account::class.java)
            .event
            .loginInfo
            .observe(this, Observer { loginInfo ->
                findViewById<TextView>(R.id.main_content).apply {
                    text = """
                        Welcome ${loginInfo?.userName} ~
                    """.trimIndent()
                }
            })

        // Result Api示例：修改用户名
        findViewById<Button>(R.id.main_btn_modify).setOnClickListener {
            val userName = P2M.apiOf(Account::class.java).event.loginInfo.getValue()?.userName?:return@setOnClickListener
            modifyAccountNameLauncherForActivityResult
                .launchChannel { userName }
                .navigation()
        }

        // 拦截器示例
        findViewById<Button>(R.id.main_btn_mall).setOnClickListener {
            P2M.apiOf(Main::class.java)
                .launcher
                .activityOfMall
                .launchChannel { intent ->
                    startActivity(intent)
                }
                .redirectionMode(ChannelRedirectionMode.CONTINUOUS_AND_RECOVER_TRY)
                .navigation()
        }

        // 退出登录
        findViewById<Button>(R.id.main_btn_logout).setOnClickListener {
            P2M.apiOf(Account::class.java)
                .service
                .logout(this)
            finish()
        }

        // 测试事件的外部可变性
        P2M.apiOf(Account::class.java)
            .event
            .testMutableEventFromExternal
            .setValue(1)
    }

}