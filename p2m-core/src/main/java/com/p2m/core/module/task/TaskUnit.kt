package com.p2m.core.module.task

/**
 * Task unit of a task, can add some dependency.
 */
interface TaskUnit {
    
    fun getOwnerClass(): Class<out Task<*, *>>

    /**
     * Add the given dependency to this task.
     *
     * @param taskClass The dependency to add to this task.
     */
    fun dependOn(taskClass: Class<out Task<*, *>>): TaskUnit

    /**
     * Adds the given dependencies to this task.
     *
     * @param taskClass The dependencies to add to this task.
     */
    fun dependOn(vararg taskClass: Class<out Task<*, *>>): TaskUnit


    fun getDependencies(): Set<Class<out Task<*, *>>>
    
}