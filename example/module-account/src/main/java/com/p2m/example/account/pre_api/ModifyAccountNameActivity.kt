package com.p2m.example.account.pre_api

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.example.account.ModifyUserNameActivityResultContract
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.R
import com.p2m.example.http.Http
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.impl.mutable

import java.util.*

/**
 * 修改帐号名Activity
 */
@ApiLauncher(launcherName = "ModifyAccountName", activityResultContract = ModifyUserNameActivityResultContract::class)
class ModifyAccountNameActivity : AppCompatActivity() {
    private var loading: ProgressBar? = null
    private var confirm: Button? = null
    private var username: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_modify_user_name)
        initView()

        confirm?.setOnClickListener {
            confirm(username?.text.toString())
        }
    }

    private fun confirm(userName: String) {
        loading?.visibility = View.VISIBLE
        // 模拟修改成功
        Http.request {
            runOnUiThread {
                onModifySuccess(userName)
            }
        }
    }

    private fun onModifySuccess(userName:String) {
        loading?.visibility = View.GONE

        P2M.apiOf(Account::class.java).event.run {
            loginInfo.getValue()?.run {

                // 更新本地缓存
                val userDiskCache = UserDiskCache(this@ModifyAccountNameActivity)
                this.userName = userName
                userDiskCache.saveLoginUserInfo(this)
                // 发送事件
                mutable().loginInfo.setValue(this)

                // 设置结果
                setResult(RESULT_OK, Intent().apply {
                    putExtra("result_new_user_name", userName)
                })
            }
        }

        finish()
    }

    private fun initView() {
        loading = findViewById<ProgressBar>(R.id.loading)
        username = findViewById<EditText>(R.id.username)
        confirm = findViewById<Button>(R.id.confirm)
        username?.afterTextChanged {
            confirm?.isEnabled = username?.text?.length?:0 > 2
        }

        val currentName = intent.getStringExtra("current_user_name")
        findViewById<TextView>(R.id.tv_current_name)?.text = "当前用户名：$currentName"
    }
}