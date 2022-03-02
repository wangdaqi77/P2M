package com.p2m.example.account.initialization

import android.content.Context
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.example.account.pre_api.LoginUserInfo
import com.p2m.example.account.UserDiskCache

// 读取登录用户信息的任务，input:UserDiskCache output:LoginUserInfo
class LoadLastUserTask: Task<UserDiskCache, LoginUserInfo?>() {

    // 处于执行阶段，运行在单独子线程，LoadLoginStateTask执行完才会执行这里
    override fun onExecute(context: Context, input: UserDiskCache, taskOutputProvider: TaskOutputProvider): LoginUserInfo? {
        val loginState = taskOutputProvider.outputOf(LoadLoginStateTask::class.java)
        // 输出查询到的用户信息
        return if (loginState) input.readLoginUserInfo() else null
    }
} 