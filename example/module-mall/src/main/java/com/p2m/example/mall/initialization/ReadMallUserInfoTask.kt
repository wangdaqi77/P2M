package com.p2m.example.mall.initialization

import android.content.Context
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.example.mall.MallDiskCache
import com.p2m.example.mall.pre_api.MallUserInfo

class ReadMallUserInfoTask :Task<MallDiskCache, MallUserInfo?>() {
    override fun onExecute(context: Context, input: MallDiskCache, taskOutputProvider: TaskOutputProvider): MallUserInfo? {
        return input.readMallUserInfo()
    }

}