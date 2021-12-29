package com.p2m.core.internal.module.task

import com.p2m.core.module.task.Task
import com.p2m.core.module.task.TaskUnit

internal class TaskUnitImpl constructor(private val owner: Class<out Task<*, *>>, val input: Any?, val ownerInstance: Task<*, *>) : TaskUnit {
    private val dependencies = hashSetOf<Class<out Task<*, *>>>()

    override fun getOwnerClass(): Class<out Task<*, *>> = owner

    override fun dependOn(taskClass: Class<out Task<*, *>>): TaskUnit {
        dependencies.add(taskClass)
        return this
    }
    
    override fun dependOn(vararg taskClass: Class<out Task<*, *>>): TaskUnit {
        dependencies.addAll(taskClass)
        return this
    }

    override fun getDependencies(): Set<Class<out Task<*, *>>> = dependencies

}