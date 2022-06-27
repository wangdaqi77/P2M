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
import com.p2m.core.api
import com.p2m.core.launcher
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
                    RESULT_OK -> Toast.makeText(this, "(ResultApi示例) 结果：成功\n数据：$output", Toast.LENGTH_SHORT).show()
                    else ->      Toast.makeText(this, "(ResultApi示例) 结果：失败", Toast.LENGTH_SHORT).show()
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
                        用户名:${loginInfo?.userName}
                        
                        手机号:${loginInfo?.phone}
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
                .activityOfInterceptor
                .launchChannel(::startActivity)
                .navigation()
        }

        // 开启服务
        findViewById<Button>(R.id.main_btn_start_service).setOnClickListener {
            P2M.apiOf(Account::class.java)
                .launcher
                .serviceOfTest
                .launchChannel(::startService)
                .navigation()
        }

        // 停止服务
        findViewById<Button>(R.id.main_btn_stop_service).setOnClickListener {
            Account::class.launcher.serviceOfTest.stop(this)
//            等同于
//            P2M.apiOf(Account::class.java)
//                .launcher
//                .serviceOfTest
//                .stop(this)
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
            .testExternalMutable
            .setValue(1)
    }

}