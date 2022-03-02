package com.p2m.example.mall.initialization

import android.content.Context
import com.p2m.annotation.module.ModuleInitializer
import com.p2m.core.P2M
import com.p2m.core.module.*
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister
import com.p2m.example.mall.MallDiskCache
import com.p2m.example.mall.p2m.api.Mall
import com.p2m.example.mall.p2m.impl.mutable

@ModuleInitializer
class MallModuleInit : ModuleInit {

    override fun onEvaluate(context: Context, taskRegister: TaskRegister) {
        val mallDiskCache = MallDiskCache(context)
        taskRegister.register(ReadMallUserInfoTask::class.java, input = mallDiskCache)
    }

    override fun onCompleted(context: Context, taskOutputProvider: TaskOutputProvider) {
        val mallUserInfoTask = taskOutputProvider.outputOf(ReadMallUserInfoTask::class.java)
        P2M.apiOf(Mall::class.java)
            .event
            .mutable()
            .mallUserInfo
            .setValue(mallUserInfoTask)
    }
}