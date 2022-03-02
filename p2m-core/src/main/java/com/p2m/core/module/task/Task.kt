package com.p2m.core.module.task

import android.content.Context
import androidx.annotation.WorkerThread
import com.p2m.core.internal.NULL
import com.p2m.core.module.ModuleInit

/**
 * A task is the smallest unit in a module to perform necessary initialization.
 *
 * It is design for fast complete necessary initialization.
 *
 * Note:
 *  * Only recommended to execute lightweight work, because will block the main thread
 *  if `onExecute` running too long.
 *
 * For example, In order to provide necessary data for external, the login status needs
 * to be loaded when `Account` module is initialize.
 * ```kotlin
 * @ModuleInitializer
 * class AccountModuleInit : ModuleInit {
 *     override fun onEvaluate(context: Context, taskRegister: TaskRegister) {
 *         val userDiskCache = UserDiskCache(context)
 *         // register task.
 *         taskRegister.register(LoadLoginStateTask::class.java, input = userDiskCache)
 *     }
 *
 *     override fun onCompleted(context: Context, taskOutputProvider: TaskOutputProvider) {
 *         // get output of LoadLoginStateTask
 *         val loginState = taskOutputProvider.outputOf(LoadLoginStateTask::class.java)
 *
 *         // input data in its api...
 *     }
 * }
 *
 * class LoadLastUserTask: Task<UserDiskCache, Boolean>() {
 *     override fun onExecute(context: Context, input: UserDiskCache, taskOutputProvider: TaskOutputProvider): Boolean {
 *         return input.readLoginState()
 *     }
 * }
 * ```
 *
 * @param INPUT corresponds type of input.
 * @param OUTPUT corresponds type of output.
 */
abstract class Task<INPUT, OUTPUT> {
    internal var inputObj: Any? = NULL

    internal var outputObj : Any? = NULL

    internal val output: OUTPUT
        get() = NULL.unbox(outputObj)

    /**
     * Execution stage, means start executing the registered task within a module.
     *
     * Running on alone work thread.
     *
     * @param input input passed in during registration, see [TaskRegister.register].
     * @param taskOutputProvider task output provider, call `taskOutputProvider.outputOf`
     * can get output of dependency.
     *
     * @return output.
     *
     * @see TaskOutputProvider TaskOutputProvider - get output of dependency.
     */
    @WorkerThread
    abstract fun onExecute(context: Context, input: INPUT, taskOutputProvider: TaskOutputProvider): OUTPUT

    @WorkerThread
    internal fun onExecute(context: Context, taskOutputProvider: TaskOutputProvider): OUTPUT =
        onExecute(context, NULL.unbox(inputObj), taskOutputProvider)

    override fun toString(): String {
        return this::class.java.simpleName
    }
}