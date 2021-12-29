package com.p2m.core.module.task

/**
 * Task register.
 */
interface TaskRegister {

    /**
     * Register a task.
     *
     * @param clazz the task class.
     * @param input [Task.input] of [clazz] instance set to the value.
     */
    fun<INPUT> register(clazz: Class<out Task<INPUT, *>>, input: INPUT): TaskUnit

    /**
     *
     * Register some task.
     *
     * @param clazz the task class.
     */
    fun registers(vararg clazz: Class<out Task<*, *>>)

    /**
     * Found a task that has been registered.
     */
    fun find(clazz: Class<out Task<*, *>>): TaskUnit?
    
}