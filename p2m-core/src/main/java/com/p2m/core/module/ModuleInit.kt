package com.p2m.core.module

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskUnit
import com.p2m.annotation.module.ModuleInitializer

/**
 * A [Module] has one [ModuleInit] only, related to [ModuleInitializer].
 *
 * Module initialization has three stages, in the following order:
 *  * evaluate, corresponds to [onEvaluate].
 *  * execute,  corresponds to [Task.onExecute].
 *  * executed, corresponds to [onCompleted].
 *
 * The module initialization has the following formula:
 *  * Within a module, the execution order must be
 *  [onEvaluate] > [Task.onExecute] > [onCompleted].
 *  * Within a module, If task A depends on task B, the execution order must be
 *  `onExecute` of task B > `onExecute` of task A.
 *  * If module A depends on module B, the execution order must be
 *  [onCompleted] of module B > [onCompleted] of module A.
 *  * If module A depends on module B and B depends on C, the execution order must be
 *  [onCompleted] of module C > [onCompleted] of module A.
 *
 * Begin initialization for all module by call `P2M.init()` in your custom application.
 *
 * see more at [doc](https://github.com/wangdaqi77/P2M)
 *
 * @see onEvaluate - evaluate stage.
 * @see onCompleted - executed stage.
 * @see Task - the smallest unit in a module to perform initialization.
 * @see TaskRegister - register some task.
 * @see TaskOutputProvider - get some task output.
 */
interface ModuleInit : OnEvaluateListener, OnCompletedListener

interface OnEvaluateListener{
    /**
     * Evaluate stage of itself.
     *
     * Here, you can use [TaskRegister] to register some task and organizational dependencies
     * for help initialize module fast, and then these tasks will be executed
     * on different worker threads in the order of dependencies.
     *
     * Note:
     *  * Can not call `P2M.apiOf()` here.
     *  * It running in work thread.
     *
     * @param taskRegister task register.
     *
     * @see TaskRegister TaskRegister - register some task.
     * @see Task Task - is the smallest unit in a module to perform initialization.
     */
    @WorkerThread
    fun onEvaluate(context: Context, taskRegister: TaskRegister)
}

interface OnCompletedListener{
    /**
     * Executed stage of itself, indicates will completed initialized of the module.
     *
     * Called when its all tasks be completed and all dependencies completed initialized.
     *
     * Here, you can use [TaskOutputProvider] to get some output of itself tasks.
     *
     * Note:
     *  * It running in main thread.
     *  * The correct data must be entered in the `Api` area before the module been completed
     *  initialized, that can the module be used safely by external modules.
     *
     * @param taskOutputProvider task output provider.
     *
     * @see TaskOutputProvider TaskOutputProvider - get some task output.
     */
    @MainThread
    fun onCompleted(context: Context, taskOutputProvider: TaskOutputProvider)
}

class EmptyModuleInit : ModuleInit {

    override fun onEvaluate(context: Context, taskRegister: TaskRegister) { }

    override fun onCompleted(context: Context, taskOutputProvider: TaskOutputProvider) { }
}
