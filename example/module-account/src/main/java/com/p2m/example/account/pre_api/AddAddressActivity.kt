package com.p2m.example.account.pre_api

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.R
import com.p2m.example.http.Http
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.impl.mutable

import java.util.*

/**
 * 添加收货地址Activity
 */
@ApiLauncher(
    launcherName = "AddAddress",
    activityResultContract = SimpleBooleanResultContract::class
)
class AddAddressActivity : AppCompatActivity() {
    private var loading: ProgressBar? = null
    private var confirm: Button? = null
    private var back: Button? = null
    private var etAddress: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_add_address)
        initView()

        confirm?.setOnClickListener {
            confirm(etAddress?.text.toString())
        }

        back?.setOnClickListener {
            finish()
        }
    }

    private fun confirm(phone: String) {
        loading?.visibility = View.VISIBLE
        // 模拟成功
        Http.request {
            runOnUiThread {
                onModifySuccess(phone)
            }
        }
    }

    private fun onModifySuccess(address:String) {
        loading?.visibility = View.GONE

        P2M.apiOf(Account::class.java).event.run {
            loginInfo.getValue()?.run {

                // 更新本地缓存
                val userDiskCache = UserDiskCache(this@AddAddressActivity)
                this.address = address
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
        etAddress = findViewById<EditText>(R.id.et_address)
        confirm = findViewById<Button>(R.id.confirm)
        back = findViewById<Button>(R.id.tv_back)
        etAddress?.afterTextChanged {
            confirm?.isEnabled = etAddress?.text?.length?:0 > 2
        }

    }
}