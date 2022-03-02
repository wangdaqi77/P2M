package com.p2m.core.module

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.TaskRegister
import com.p2m.core.module.task.Task
import com.p2m.annotation.module.ModuleInitializer

/**
 * A [Module] has one [ModuleInit] only, related to [ModuleInitializer].
 *
 * Module initialization has three stages in sequence:
 *  * evaluation stage, corresponds to [onEvaluate].
 *  * execution stage,  corresponds to [Task.onExecute].
 *  * completion stage, corresponds to [onCompleted].
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
 * Start initialize for all module by call `P2M.init()` in your custom application.
 *
 * see more at [doc](https://github.com/wangdaqi77/P2M)
 *
 * @see onEvaluate - evaluation stage.
 * @see onCompleted - completion stage.
 * @see Task - the smallest unit in a module to perform initialization.
 * @see TaskRegister - register some task.
 * @see TaskOutputProvider - get some task output.
 */
interface ModuleInit : OnEvaluateListener, OnCompletedListener

interface OnEvaluateListener{
    /**
     * Evaluate stage, means ready to start initialization.
     *
     * Running on a alone work thread.
     *
     * Used [taskRegister] to register tasks and organize the dependencies of tasks
     * in this module, these tasks are designed for fast loading of
     * data, which will be used in the initialization completion stage.
     *
     * Can not call `P2M.apiOf()` here
     *
     * @param taskRegister register tasks.
     *
     * @see Task Task - is the smallest unit in a module to perform initialization.
     */
    @WorkerThread
    fun onEvaluate(context: Context, taskRegister: TaskRegister)
}

interface OnCompletedListener{
    /**
     * Completion stage, means initialization is complete of the module.
     *
     * Running on main thread.
     *
     * Called when its all tasks be completed and all dependencies completed initialized.
     *
     * Here, you can use [TaskOutputProvider] get output data and load the data into api,
     * that can the module be used safely by external modules.
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
