package com.p2m.example.account.module_init

import android.content.Context
import com.p2m.annotation.module.ModuleInitializer
import com.p2m.core.P2M
import com.p2m.core.module.*
import com.p2m.example.account.p2m.api.Account
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister
import com.p2m.core.module.task.TaskUnit
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.p2m.impl.mutable

@ModuleInitializer
class AccountModuleInit : ModuleInit {

    // 运行在子线程，用于注册该模块内的任务、组织任务的依赖关系，所有的任务在单独的子线程运行。
    override fun onEvaluate(context: Context, taskRegister: TaskRegister) {
        val userDiskCache = UserDiskCache(context) // 用户本地缓存

        // 注册读取登录状态的任务
        taskRegister.register(LoadLoginStateTask::class.java, userDiskCache)

        // 注册读取登录用户信息的任务
        taskRegister
            .register(LoadLastUserTask::class.java, userDiskCache)
            .dependOn(LoadLoginStateTask::class.java) // 执行顺序一定为LoadLoginStateTask.onExecute() > LoadLastUserTask.onExecute()
    }

    // 运行在主线程，当所有的依赖项完成模块初始化且本模块的任务执行完毕时调用
    override fun onExecuted(context: Context, taskOutputProvider: TaskOutputProvider) {
        val loginState = taskOutputProvider.outputOf(LoadLoginStateTask::class.java) // 获取任务输出-登录状态
        val loginInfo = taskOutputProvider.outputOf(LoadLastUserTask::class.java)

        // 在该模块初始化完成时务必对其Api区输入正确的数据，只有这样才能保证其他模块安全的使用该模块。
        val account = P2M.apiOf(Account::class.java)                        // 找到自身的Api区
        account.event.mutable().loginState.setValue(loginState)             // 保存到事件持有者
        account.event.mutable().loginInfo.setValue(loginInfo)               // 保存到事件持有者
    }
}