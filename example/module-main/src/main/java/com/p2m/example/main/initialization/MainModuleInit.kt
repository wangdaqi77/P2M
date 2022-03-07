package com.p2m.example.main.initialization

import android.content.Context
import android.content.Intent
import com.p2m.annotation.module.ModuleInitializer
import com.p2m.core.P2M
import com.p2m.core.event.BackgroundObserver
import com.p2m.core.module.*
import com.p2m.example.account.p2m.api.Account
import com.p2m.example.main.p2m.api.Main
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister

@ModuleInitializer
class MainModuleInit : ModuleInit {

    override fun onEvaluate(context: Context, taskRegister: TaskRegister) {

    }

    // 运行在主线程，当模块的依赖项完成初始化且本模块的任务执行完毕时调用
    override fun onCompleted(context: Context, taskOutputProvider: TaskOutputProvider) {
        val account = P2M.apiOf(Account::class.java)
        
        // 登录成功跳转主页
        account.event.loginSuccess.observeForeverNoSticky(BackgroundObserver<Unit> {
                // 登录成功启动主界面
            P2M.apiOf(Main::class.java)
                .launcher
                .activityOfMain
                .launchChannel {
                    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                .navigation()
        })
    }

}