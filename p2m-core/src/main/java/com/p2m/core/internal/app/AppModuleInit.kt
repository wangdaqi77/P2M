package com.p2m.core.internal.app

import android.content.Context
import com.p2m.core.P2M
import com.p2m.core.app.App
import com.p2m.core.module.ModuleInit
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister

internal class AppModuleInit : ModuleInit {
    override fun onEvaluate(context: Context, taskRegister: TaskRegister) {

    }

    override fun onExecuted(context: Context, taskOutputProvider: TaskOutputProvider) {
        P2M.apiOf(App::class.java)
            .service
            .init(context)
    }
}