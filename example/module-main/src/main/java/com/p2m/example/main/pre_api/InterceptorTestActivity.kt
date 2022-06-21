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
import com.p2m.core.channel.*
import com.p2m.core.exception.ChannelInterruptedWhenRedirectException
import com.p2m.core.launcher.LaunchActivityChannel
import com.p2m.example.main.R
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForBindPhoneNum
import com.p2m.example.mall.p2m.api.Mall
import com.p2m.example.mall.p2m.api.MallLaunchActivityInterceptorForAddAddress

@ApiLauncher("Interceptor")
class InterceptorTestActivity : AppCompatActivity() {

    /*测试模式：CONSERVATIVE*/
    private var recoverableChannelForTestCONSERVATIVE : RecoverableChannel? = null
    private val bindPhoneResultLauncherForTestCONSERVATIVE = P2M.apiOf(Account::class.java).launcher
        .activityOfBindPhone
        .registerResultLauncher(this) { resultCode, _ ->
            if (resultCode == RESULT_OK) {
                // 成功恢复导航
                printLog("成功绑定手机后 recoverNavigation()")
                recoverableChannelForTestCONSERVATIVE?.recoverNavigation()
            }
        }
    private val addAddressResultLauncherForTestCONSERVATIVE = P2M.apiOf(Mall::class.java).launcher
        .activityOfAddAddress
        .registerResultLauncher(this) { resultCode, _ ->
            if (resultCode == RESULT_OK) {
                // 成功恢复导航
                printLog("成功添加收货地址后 recoverNavigation()")
                recoverableChannelForTestCONSERVATIVE?.recoverNavigation()
            }
        }

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
            P2M.apiOf(Mall::class.java)
                .service
                .deleteAddress(this)
            printLog("删除收货地址")
        }
    }

    private fun testGreenChannel() {
        P2M.apiOf(Mall::class.java)
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
        P2M.apiOf(Mall::class.java)
            .launcher
            .activityOfMall
            .launchChannel(::startActivity)
            .redirectionMode(ChannelRedirectionMode.CONSERVATIVE)
            .navigation(object : NavigationCallback {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("onStarted() 跳转商城\r\n" +
                            "拦截器的重定向模式为ChannelRedirectionMode.CONSERVATIVE，该模式在拦截器中重定向就会中断并触发[onInterrupt]，永远不会触发[onRedirect]")
                }

                override fun onCompleted(channel: Channel) {
                    printLog("onCompleted()")
                }

                override fun onInterrupt(channel: Channel, e: Throwable) {
                    printLog("onInterrupt(), message:${e.message}")
                    if (e is ChannelInterruptedWhenRedirectException) {
                        val recoverableChannel = e.recoverableChannel // 可恢复的通道
                        if (recoverableChannel.isInterruptedFrom(AccountLaunchActivityInterceptorForBindPhoneNum::class)) {
                            printLog("重定向到绑定手机界面时中断，提示需要绑定手机")
                            AlertDialog.Builder(this@InterceptorTestActivity)
                                .setTitle("跳转商城中断")
                                .setMessage("请先绑定手机！")
                                .setNegativeButton("取消") { dialog, _ ->
                                    dialog.dismiss()
                                    printLog("取消")
                                }
                                .setPositiveButton("绑定") { dialog, _ ->
                                    dialog.dismiss()
                                    printLog("启动绑定手机")

                                    recoverableChannelForTestCONSERVATIVE = recoverableChannel
                                    // ResultApi 启动绑定手机号
                                    bindPhoneResultLauncherForTestCONSERVATIVE
                                        .launchChannel { }
                                        .navigation()
                                }
                                .create()
                                .show()
                        } else if (recoverableChannel.isInterruptedFrom(MallLaunchActivityInterceptorForAddAddress::class)) {
                            printLog("重定向到添加收货地址界面时中断，提示需要添加收货地址")
                            AlertDialog.Builder(this@InterceptorTestActivity)
                                .setTitle("跳转商城中断")
                                .setMessage("请先添加收货地址！")
                                .setNegativeButton("取消") { dialog, _ ->
                                    dialog.dismiss()
                                    printLog("取消")
                                }
                                .setPositiveButton("添加") { dialog, _ ->
                                    dialog.dismiss()
                                    printLog("启动添加收货地址")

                                    recoverableChannelForTestCONSERVATIVE = recoverableChannel
                                    // ResultApi 启动添加收货地址
                                    addAddressResultLauncherForTestCONSERVATIVE
                                        .launchChannel{ }
                                        .navigation()
                                }
                                .create()
                                .show()
                        }
                    }
                }

                override fun onRedirect(channel: Channel, redirectChannel: Channel) { }
            })
    }

    private fun testRedirectionMode2() {
        P2M.apiOf(Mall::class.java)
            .launcher
            .activityOfMall
            .launchChannel(::startActivity)
            .redirectionMode(ChannelRedirectionMode.FLEXIBLY)
            .navigation(object : NavigationCallback {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("onStarted() 跳转商城\r\n" +
                            "拦截器的重定向模式为ChannelRedirectionMode.FLEXIBLY，该模式在拦截器中可以重定向，重定向时会触发[onRedirect]，但是如果在恢复导航（重定向目标界面在销毁时会触发恢复导航）时被同一个拦截器重定向到同一个Channel时将会中断并触发[onInterrupt]")
                }

                override fun onCompleted(channel: Channel) {
                    printLog("onCompleted()")
                }

                override fun onInterrupt(channel: Channel, e: Throwable) {
                    printLog("onInterrupt(), message:${e.message}")
                    if (e is ChannelInterruptedWhenRedirectException) {
                        val recoverableChannel = e.recoverableChannel // 可恢复的通道
                        if (recoverableChannel.isInterruptedFrom(AccountLaunchActivityInterceptorForBindPhoneNum::class)) {
                            printLog("重定向到绑定手机界面时中断，用户取消了绑定手机")
                        } else if (recoverableChannel.isInterruptedFrom(MallLaunchActivityInterceptorForAddAddress::class)) {
                            printLog("重定向到添加收货地址界面时中断，用户取消了添加收货地址")
                        }
                    }
                }

                override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                    printLog("onRedirect()")
                    redirectChannel as LaunchActivityChannel
                    when (redirectChannel.activityLauncher) {
                        P2M.apiOf(Account::class.java).launcher.activityOfBindPhone -> toast("需要绑定手机号~")
                        P2M.apiOf(Mall::class.java).launcher.activityOfAddAddress -> toast("需要添加收货地址~")
                    }
                }

            })

    }

    private fun testRedirectionMode3() {
        P2M.apiOf(Mall::class.java)
            .launcher
            .activityOfMall
            .launchChannel(::startActivity)
            .redirectionMode(ChannelRedirectionMode.RADICAL)
            .navigation(object : NavigationCallback {
                override fun onStarted(channel: Channel) {
                    printLog("")
                    printLog("onStarted() 跳转商城\r\n" +
                            "拦截器的重定向模式为ChannelRedirectionMode.RADICAL，该模式在拦截器中可以重定向，重定向直到导航完成，重定向时会触发[onRedirect]，可以在重定向的目标界面点返回测试")
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
                    when (redirectChannel.activityLauncher) {
                        P2M.apiOf(Account::class.java).launcher.activityOfBindPhone -> toast("一定要先绑定手机号!")
                        P2M.apiOf(Mall::class.java).launcher.activityOfAddAddress -> toast("一定要先添加收货地址!")
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