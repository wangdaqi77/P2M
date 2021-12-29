package com.p2m.example.account.module_init

import android.content.Context
import android.util.Log
import com.p2m.core.P2M
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.example.account.pre_api.LoginUserInfo
import com.p2m.example.account.UserDiskCache
import com.p2m.example.account.p2m.api.Account
import kotlin.concurrent.thread

// 读取登录用户信息的任务，input:UserDiskCache output:LoginUserInfo
class LoadLastUserTask: Task<UserDiskCache, LoginUserInfo?>() {

    // 运行在子线程，当所有的依赖项完成模块初始化且所有注册的任务执行完毕时调用
    override fun onExecute(context: Context, taskOutputProvider: TaskOutputProvider): LoginUserInfo? {
        val loginState = taskOutputProvider.outputOf(LoadLoginStateTask::class.java)

        // 查询用户信息
        return if (loginState) {
            val userDiskCache = input
            userDiskCache.readLoginUserInfo()
        } else {
            null
        }
    }
} 