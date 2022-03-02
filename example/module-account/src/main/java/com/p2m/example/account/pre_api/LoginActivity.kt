package com.p2m.example.account.pre_api

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
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
 * 登录Activity
 *
 * 登录成功后将发送登录成功的事件[AccountEvent.loginSuccess]，外部模块接收此事件可进行跳转。
 */
@ApiLauncher("Login")
class LoginActivity : AppCompatActivity() {
    private var loading: ProgressBar? = null
    private var login: Button? = null
    private var username: EditText? = null
    private var password: EditText? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_activity_login)
        initView()
        
        login?.setOnClickListener {
            login(username?.text.toString(), password?.text.toString())
        }
    }

    private fun login(userName: String, pwd: String) {
        loading?.visibility = View.VISIBLE
        // 模拟登录成功
        Http.request {
            runOnUiThread {
                onLoginSuccess(userName, UUID.randomUUID().toString())
            }
        }
    }

    private fun onLoginSuccess(userName:String, id:String) {
        loading?.visibility = View.GONE
        
        val loginUserInfo = LoginUserInfo().apply {
            this.userId = id
            this.userName = userName
        }

        // 缓存到本地
        val userDiskCache = UserDiskCache(this)
        userDiskCache.saveLoginState(true)
        userDiskCache.saveLoginUserInfo(loginUserInfo)
        
        P2M.apiOf(Account::class.java).event.mutable().apply {
            loginState.setValue(true)        // 发送登录状态事件
            loginInfo.setValue(loginUserInfo)      // 发送用户信息事件
            loginSuccess.setValue(Unit)            // 发送主动登录成功事件
        }

        finish()
    }

    private fun initView() {
        loading = findViewById<ProgressBar>(R.id.loading)
        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        login = findViewById<Button>(R.id.login)
        username?.afterTextChanged {
            login?.isEnabled = username?.text?.length?:0 > 2 && password?.text?.length?:0  > 2
        }
        password?.afterTextChanged {
            login?.isEnabled = username?.text?.length?:0  > 2 && password?.text?.length?:0  > 2
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