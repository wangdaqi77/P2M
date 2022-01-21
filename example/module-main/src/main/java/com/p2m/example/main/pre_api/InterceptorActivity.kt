package com.p2m.example.main.pre_api

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.p2m.core.P2M
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.channel.Channel
import com.p2m.core.channel.ChannelRedirectionMode
import com.p2m.core.channel.NavigationCallback
import com.p2m.core.channel.SimpleNavigationCallback
import com.p2m.core.exception.ChannelRedirectInterruptedException
import com.p2m.core.launcher.LaunchActivityChannel
import com.p2m.example.main.R
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.main.p2m.api.Main

@ApiLauncher("Interceptor")
class InterceptorActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity_interceptor)


        // 绿色通道
        findViewById<Button>(R.id.main_btn).setOnClickListener {
            testGreenChannel()
        }

        // 拦截器重定向模式 - ChannelRedirectionMode.CONSERVATIVE
        findViewById<Button>(R.id.main_btn_1).setOnClickListener {
            testRedirectionMode1()
        }

        // 拦截器重定向模式 - ChannelRedirectionMode.FLEXIBLY
        findViewById<Button>(R.id.main_btn_2).setOnClickListener {
            testRedirectionMode2()

        }

        // 拦截器重定向模式 - ChannelRedirectionMode.RADICAL
        findViewById<Button>(R.id.main_btn_3).setOnClickListener {
            testRedirectionMode3()
        }

        // 解绑手机号
        findViewById<Button>(R.id.main_btn_reset_phone).setOnClickListener {
            P2M.apiOf(Account::class.java)
                .service
                .unbindPhone(this)
            printLog("解绑手机号")
        }

        // 删除收货地址
        findViewById<Button>(R.id.main_btn_reset_address).setOnClickListener {
            P2M.apiOf(Account::class.java)
                .service
                .deleteAddress(this)
            printLog("删除收货地址")
        }
    }

    private fun testGreenChannel() {
        P2M.apiOf(Main::class.java)
            .launcher
            .activityOfMall
            .launchChannel { intent ->
                startActivity(intent)
            }
            .greenChannel()
            .navigation(object : SimpleNavigationCallback() {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("跳转商城-绿色通道模式（不会参与拦截器服务）")
                    printLog("onStarted()")
                }

                override fun onCompleted(channel: Channel) {
                    printLog("onCompleted()")
                }
            })
    }

    private fun testRedirectionMode1() {
        P2M.apiOf(Main::class.java)
            .launcher
            .activityOfMall
            .launchChannel(::startActivity)
            .redirectionMode(ChannelRedirectionMode.CONSERVATIVE)
            .navigation(object : NavigationCallback {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("跳转商城-拦截器重定向模式：ChannelRedirectionMode.CONSERVATIVE，行为：只要重定向就会中断")
                    printLog("onStarted()")
                }

                override fun onCompleted(channel: Channel) {
                    printLog("onCompleted()")
                }

                override fun onInterrupt(channel: Channel, e: Throwable) {
                    printLog("onInterrupt(), message:${e.message}")
                    if (e is ChannelRedirectInterruptedException) {
                        val recoverableChannel = e.recoverableChannel
                        val interruptedChannel = recoverableChannel.interruptedChannel as LaunchActivityChannel
                        val bindPhoneLauncher = P2M.apiOf(Account::class.java).launcher.activityOfBindPhone
                        val addAddressLauncher = P2M.apiOf(Account::class.java).launcher.activityOfAddAddress
                        when(interruptedChannel.launcher) {
                            bindPhoneLauncher -> {
                                AlertDialog.Builder(this@InterceptorActivity)
                                    .setTitle("跳转商城中断")
                                    .setMessage("请先绑定手机！")
                                    .setNegativeButton("取消") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .setPositiveButton("好的") { dialog, _ ->
                                        dialog.dismiss()
                                        bindPhoneLauncher
                                            .launchChannel(::startActivity)
                                            .navigation()
                                    }
                                    .create()
                                    .show()
                            }
                            addAddressLauncher -> {
                                AlertDialog.Builder(this@InterceptorActivity)
                                    .setTitle("跳转商城中断")
                                    .setMessage("请先添加收货地址！")
                                    .setNegativeButton("取消") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .setPositiveButton("好的") { dialog, _ ->
                                        dialog.dismiss()
                                        addAddressLauncher
                                            .launchChannel(::startActivity)
                                            .navigation()
                                    }
                                    .create()
                                    .show()
                            }
                        }
                    }
                }

                override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                    printLog("onRedirect()")
                }
            })
    }

    private fun testRedirectionMode2() {
        P2M.apiOf(Main::class.java)
            .launcher
            .activityOfMall
            .launchChannel(::startActivity)
            .redirectionMode(ChannelRedirectionMode.FLEXIBLY)
            .navigation(object : NavigationCallback {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("跳转商城-拦截器重定向模式：ChannelRedirectionMode.FLEXIBLY，行为：可以重定向，但是如果在恢复导航时被同一个拦截器重定向到同一个Channel，则中断。")
                    printLog("onStarted()")
                }

                override fun onCompleted(channel: Channel) {
                    printLog("onCompleted()")
                }

                override fun onInterrupt(channel: Channel, e: Throwable) {
                    printLog("onInterrupt(), message:${e.message}")
                }

                override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                    printLog("onRedirect()")
                    redirectChannel as LaunchActivityChannel
                    when (redirectChannel.launcher) {
                        P2M.apiOf(Account::class.java).launcher.activityOfBindPhone -> toast("需要绑定手机号~")
                        P2M.apiOf(Account::class.java).launcher.activityOfAddAddress -> toast("需要添加收货地址~")
                    }
                }

            })

    }

    private fun testRedirectionMode3() {
        P2M.apiOf(Main::class.java)
            .launcher
            .activityOfMall
            .launchChannel(::startActivity)
            .redirectionMode(ChannelRedirectionMode.RADICAL)
            .navigation(object : NavigationCallback {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("跳转商城-拦截器重定向模式：ChannelRedirectionMode.RADICAL，行为：重定向直到导航完成，不信你点返回试试。")
                    printLog("onStarted()")
                }

                override fun onCompleted(channel: Channel) {
                    printLog("onCompleted()")
                }

                override fun onInterrupt(channel: Channel, e: Throwable) {
                    printLog("onInterrupt(), message:${e.message}")
                }

                override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                    printLog("onRedirect()")
                    redirectChannel as LaunchActivityChannel
                    when (redirectChannel.launcher) {
                        P2M.apiOf(Account::class.java).launcher.activityOfBindPhone -> toast("一定要先绑定手机号!")
                        P2M.apiOf(Account::class.java).launcher.activityOfAddAddress -> toast("一定要先添加收货地址!")
                    }
                }
            })
    }

    private fun toast(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }
    private fun printLog(content: String) {
        val finalContent = "$content\r\n"
        Log.e("拦截器示例", finalContent)
        findViewById<TextView>(R.id.main_content).append(finalContent)
    }
}