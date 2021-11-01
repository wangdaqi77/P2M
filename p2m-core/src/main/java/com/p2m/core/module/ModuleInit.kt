package com.p2m.core.module

import android.content.Context
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskUnit

/**
 * A [Module] has one [ModuleInit] only.
 *
 * Module initialization has three stages, in the following order:
 *  * evaluate, corresponds to [onEvaluate].
 *  * execute,  corresponds to [Task.onExecute].
 *  * executed, corresponds to [onExecuted].
 *
 * The module initialization has the following formula:
 *  * Within a module, the execution order must be
 *  [onEvaluate] > [Task.onExecute] > [onExecuted].
 *  * Within a module, If task A depends on task B, the execution order must be
 *  `onExecute` of task B > `onExecute` of task A.
 *  * If module A depends on module B, the execution order must be
 *  [onExecuted] of module B > [onExecuted] of module A.
 *  * If module A depends on module B and B depends on C, the execution order must be
 *  [onExecuted] of module C > [onExecuted] of module A.
 *
 * Begin initialization for all module by call `P2M.driverBuilder().build().open()`
 * in your custom application.
 *
 * see more at https://github.com/wangdaqi77/P2M
 *
 * @see onEvaluate onEvaluate - evaluate stage.
 * @see onExecuted onExecuted - executed stage.
 * @see TaskRegister TaskRegister - register some task.
 * @see Task Task - is the smallest unit in a module to perform initialization.
 * @see TaskOutputProvider TaskOutputProvider - get some task output.
 * @see SafeModuleApiProvider SafeModuleApiProvider - get some module api.
 *
 */
interface ModuleInit : OnEvaluateListener, OnExecutedListener

interface OnEvaluateListener{
    /**
     * Evaluate stage of itself.
     *
     * Here, you can use [TaskRegister] to register some task for help initialize module
     * fast, and then these tasks will be executed in the order of dependencies.
     *
     * Note, it running in work thread.
     *
     * @param taskRegister task register.
     *
     * @see TaskRegister TaskRegister - register some task.
     * @see Task Task - is the smallest unit in a module to perform initialization.
     */
    fun onEvaluate(context: Context, taskRegister: TaskRegister<out TaskUnit>)
}

interface OnExecutedListener{
    /**
     * Executed stage of itself, indicates will completed initialized of the module.
     *
     * Called when its all tasks be completed and all dependencies completed initialized.
     *
     * Here, you can use [TaskOutputProvider] to get some output of itself tasks,
     * also use [SafeModuleApiProvider] to get some dependency module api.
     *
     * Note, it running in main thread.
     *
     * @param taskOutputProvider task output provider.
     * @param moduleApiProvider module provider.
     *
     * @see TaskOutputProvider TaskOutputProvider - get some task output.
     * @see SafeModuleApiProvider SafeModuleApiProvider - get some module.
     */
    fun onExecuted(context: Context, taskOutputProvider: TaskOutputProvider, moduleApiProvider: SafeModuleApiProvider)
}

class EmptyModuleInit : ModuleInit {

    override fun onEvaluate(context: Context, taskRegister: TaskRegister<out TaskUnit>) { }

    override fun onExecuted(context: Context, taskOutputProvider: TaskOutputProvider, moduleApiProvider: SafeModuleApiProvider) { }
}
