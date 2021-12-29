package com.p2m.core.internal.module.task

import com.p2m.core.internal.NULL
import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskFactory
import com.p2m.core.module.task.TaskUnit
import com.p2m.core.module.task.TaskRegister

internal interface TaskContainer<out UNIT: TaskUnit>{
    /**
     * Found a task in the container.
     */
    fun find(clazz: Class<out Task<*, *>>): UNIT?

    fun getAll(): Collection<UNIT>
}


internal class TaskContainerImpl(
    val topTaskClass: Class<out Task<*, *>>,
    private val taskFactory: TaskFactory
) : TaskRegister, TaskContainer<TaskUnitImpl> {
    private val container = HashMap<Class<out Task<*, *>>, TaskUnitImpl>()

    val onlyHasTop: Boolean
        get() = container.size == 1
    
    init {
        registers(topTaskClass)
    }

    override fun registers(vararg clazz: Class<out Task<*, *>>) {
        for (clazz1 in clazz) {
            check(!container.containsKey(clazz1)) {
                "${clazz1.canonicalName} registered already."
            }
            val task = taskFactory.newInstance(clazz1)
            val taskUnit = TaskUnitImpl(clazz1, NULL, task)
            container[clazz1] = taskUnit
        }
    }

    override fun <INPUT> register(clazz: Class<out Task<INPUT, *>>, input: INPUT): TaskUnitImpl {
        check(!container.containsKey(clazz)) {
            "${clazz.canonicalName} registered already."
        }
        @Suppress("UNCHECKED_CAST")
        val taskUnit = TaskUnitImpl(clazz, input, clazz.newInstance())
        container[clazz] = taskUnit
        return taskUnit
    }

    override fun find(clazz: Class<out Task<*, *>>): TaskUnitImpl? = container[clazz]

    override fun getAll(): Collection<TaskUnitImpl> = container.values
}