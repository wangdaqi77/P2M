package com.p2m.example.account.initialization

import android.content.Context
import com.p2m.annotation.module.ModuleInitializer
import com.p2m.core.P2M
import com.p2m.core.module.*
import com.p2m.example.account.p2m.api.Account
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.p2m.impl.mutable

@ModuleInitializer
class AccountModuleInit : ModuleInit {

    // 评估自身阶段，意味着准备开始初始化
    override fun onEvaluate(context: Context, taskRegister: TaskRegister) {
        // 用户本地缓存
        val userDiskCache = UserDiskCache(context)
        // 注册读取登录状态的任务
        taskRegister.register(LoadLoginStateTask::class.java, input = userDiskCache)

        // 注册读取登录用户信息的任务
        taskRegister.register(LoadLastUserTask::class.java, userDiskCache)
            // 执行顺序一定为LoadLoginStateTask.onExecute() > LoadLastUserTask.onExecute()
            .dependOn(LoadLoginStateTask::class.java)
    }

    // 初始化完成阶段，意味着初始化完成
    override fun onCompleted(context: Context, taskOutputProvider: TaskOutputProvider) {
        val loginState = taskOutputProvider.outputOf(LoadLoginStateTask::class.java) // 获取任务输出-登录状态
        val loginInfo = taskOutputProvider.outputOf(LoadLastUserTask::class.java)    // 获取任务输出-登录用户信息

        // Account模块初始化完成后，外部模块才可以使用其Api区，因此在初始化完成时在其Api区一定要准备好必要的数据。
        val account = P2M.apiOf(Account::class.java)                        // 找到自身的Api区
        account.event.mutable().loginState.setValue(loginState)             // 数据保存到事件持有者
        account.event.mutable().loginInfo.setValue(loginInfo)               // 数据保存到事件持有者
    }
}