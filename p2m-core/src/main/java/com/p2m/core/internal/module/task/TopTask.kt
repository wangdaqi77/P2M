package com.p2m.core.internal.module.task

import android.content.Context
import com.p2m.core.module.task.TaskOutputProvider
import com.p2m.core.module.task.Task

internal class TopTask : Task<Unit?, Unit?>() {
    override fun onExecute(
        context: Context,
        input: Unit?,
        taskOutputProvider: TaskOutputProvider
    ): Unit? = null
}