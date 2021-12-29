package com.p2m.core.internal.module.task

import com.p2m.core.exception.P2MException
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskUnit

internal class TaskOutputProviderImplForTask constructor(private val taskContainer: TaskContainerImpl, private val taskUnit: TaskUnit) : TaskOutputProvider {
    
    @Suppress("UNCHECKED_CAST")
    override fun <OUTPUT> outputOf(clazz: Class<out Task<*, OUTPUT>>): OUTPUT {

        check(taskUnit.getDependencies().contains(clazz)) {
            "${taskUnit.getOwnerClass().canonicalName} must depend on ${clazz.canonicalName}"
        }

        val task = taskContainer.find(clazz)?.ownerInstance as? Task<*, OUTPUT>
            ?: throw P2MException("not found ${clazz.name}")
        return task.output
    }
}
