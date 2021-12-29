package com.p2m.core.internal.module.task

import android.content.Context
import com.p2m.core.internal.graph.Node
import com.p2m.core.module.task.Task

internal class TaskNode constructor(
    val context: Context,
    val taskName: String,
    val task: Task<*, *>,
    val input: Any?,
    val taskProvider: TaskOutputProviderImplForTask,
    override val isTop: Boolean
) : Node<TaskNode>() {

    override val name: String
        get() = taskName
}