package com.p2m.core.internal.module

import android.content.Context
import com.p2m.core.module.ModuleInit
import com.p2m.core.module.OnEvaluateListener
import com.p2m.core.module.OnExecutedListener
import com.p2m.core.module.SafeModuleProvider
import com.p2m.core.module.task.*


internal class AppModuleInit(
    val context: Context,
    private val onEvaluateListener: OnEvaluateListener?,
    private val onExecutedListener: OnExecutedListener?
) : ModuleInit {

    override fun onEvaluate(taskRegister: TaskRegister) {
        onEvaluateListener?.onEvaluate(taskRegister)
    }

    override fun onExecuted(taskOutputProvider: TaskOutputProvider, moduleProvider: SafeModuleProvider) {
        onExecutedListener?.onExecuted(taskOutputProvider, moduleProvider)
    }
}