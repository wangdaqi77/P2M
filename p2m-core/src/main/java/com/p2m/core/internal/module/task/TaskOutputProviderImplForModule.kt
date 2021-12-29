package com.p2m.core.internal.module.task

import com.p2m.core.exception.P2MException
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.Task

internal class TaskOutputProviderImplForModule(private val taskContainer: TaskContainerImpl) : TaskOutputProvider {
    
    @Suppress("UNCHECKED_CAST")
    override fun <OUTPUT> outputOf(clazz: Class<out Task<*, OUTPUT>>): OUTPUT {
        val task = taskContainer.find(clazz)?.ownerInstance as? Task<*, OUTPUT>
            ?: throw P2MException("not found ${clazz.name}")
        return task.output
    }
}
