package com.p2m.example.account.pre_api

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.my.lib.common.SimpleBooleanResultContract
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.R
import com.p2m.example.http.Http
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.impl.mutable

import java.util.*

/**
 * 绑定手机号Activity
 */
@ApiLauncher(
    launcherName = "BindPhone",
    activityResultContract = SimpleBooleanResultContract::class
)
class BindPhoneActivity : AppCompatActivity() {
    private var loading: ProgressBar? = null
    private var confirm: Button? = null
    private var back: Button? = null
    private var etPhone: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_activity_bind_phone)
        initView()

        confirm?.setOnClickListener {
            confirm(etPhone?.text.toString())
        }

        back?.setOnClickListener {
            finish()
        }
    }

    private fun confirm(phone: String) {
        loading?.visibility = View.VISIBLE
        // 模拟绑定成功
        Http.request {
            runOnUiThread {
                onModifySuccess(phone)
            }
        }
    }

    private fun onModifySuccess(phone:String) {
        loading?.visibility = View.GONE

        P2M.apiOf(Account::class.java).event.run {
            loginInfo.getValue()?.run {

                // 更新本地缓存
                val userDiskCache = UserDiskCache(this@BindPhoneActivity)
                this.phone = phone
                userDiskCache.saveLoginUserInfo(this)
                // 发送事件
                mutable().loginInfo.setValue(this)
            }
        }
        SimpleBooleanResultContract.makeSuccess(this)
        finish()
    }

    private fun initView() {
        loading = findViewById<ProgressBar>(R.id.loading)
        etPhone = findViewById<EditText>(R.id.et_phone)
        confirm = findViewById<Button>(R.id.confirm)
        back = findViewById<Button>(R.id.tv_back)
        etPhone?.afterTextChanged {
            confirm?.isEnabled = etPhone?.text?.length?:0 > 2
        }

    }
}