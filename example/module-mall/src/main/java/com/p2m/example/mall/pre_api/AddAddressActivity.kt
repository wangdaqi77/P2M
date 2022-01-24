package com.p2m.example.mall.pre_api

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.my.lib.common.SimpleBooleanResultContract
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.example.http.Http
import com.p2m.example.mall.MallDiskCache
import com.p2m.example.mall.R
import com.p2m.example.mall.p2m.api.Mall
import com.p2m.example.mall.p2m.impl.mutable

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
        setContentView(R.layout.mall_activity_add_address)
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

        P2M.apiOf(Mall::class.java).event.run {
            mallUserInfo.getValue()?.run {

                // 更新本地缓存
                val mallDiskCache = MallDiskCache(this@AddAddressActivity)
                this.address = address
                mallDiskCache.saveMallUserInfo(this)
                // 发送事件
                mutable().mallUserInfo.setValue(this)
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

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}