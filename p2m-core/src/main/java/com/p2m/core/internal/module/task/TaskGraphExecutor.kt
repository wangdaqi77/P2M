package com.p2m.core.internal.module.task

import com.p2m.core.internal.execution.BeginDirection
import com.p2m.core.internal.execution.TagRunnable
import com.p2m.core.internal.graph.AbsGraphExecutor
import com.p2m.core.internal.graph.Stage
import com.p2m.core.internal.graph.Node.State
import com.p2m.core.internal.log.logI
import com.p2m.core.internal.module.SafeModuleApiProvider
import com.p2m.core.module.task.Task
import java.util.concurrent.*

internal class TaskGraphExecutor(
    override val graph: TaskGraph,
    private val executor: ExecutorService,
    private val executingModuleProvider: ThreadLocal<SafeModuleApiProvider>,
    direction: BeginDirection
) : AbsGraphExecutor<Class<out Task<*, *>>, TaskNode, TaskGraph>(direction) {

    override val messageQueue: BlockingQueue<Runnable> = ArrayBlockingQueue(graph.taskSize)

    override fun runNode(node: TaskNode, onDependsNodeComplete: () -> Unit) {
        node.markStarted(onDependsNodeComplete) {
            // Started
            executor.execute(TagRunnable(node.name) Runnable@{
                // Depending
                node.depending()

                if (node.isTop) {
                    node.executed()
                    onDependsNodeComplete()
                    return@Runnable
                }

                // Executing
                node.executing()

                // Completed
                node.executed()
                onDependsNodeComplete()
            })
        }
    }

    private fun TaskNode.executed() {
        markCompleted()
    }

    private fun TaskNode.depending() {
        mark(State.DEPENDING)
        if (dependNodes.isEmpty()) {
            return
        }

        val countDownLatch = CountDownLatch(dependNodes.size)
        val onDependsNodeComplete = {
            // Dependencies be Completed.
            countDownLatch.countDown()
        }

        dependNodes.forEach { dependNode ->
            runNode(dependNode, onDependsNodeComplete)
        }

        // Wait dependencies be Completed.
        countDownLatch.await()
    }

    private fun TaskNode.executing() {
        mark(State.EXECUTING)
        logI("Task-${taskName} `onExecute()`")
        task.inputObj = input
        executingModuleProvider.set(graph.safeModuleApiProvider)
        task.outputObj = task.onExecute(context, taskProvider)
        executingModuleProvider.set(null)
    }

    override fun onCompletedForGraph(graph: TaskGraph) {
        logI("${graph.moduleName}-Task Completed.")
    }

    override fun onCompletedForStage(stage: Stage<TaskNode>) {
        // if (stage.nodes?.firstOrNull()?.isTop != true) {
        //     logI("Task-Stage${stage.name} Completed.")
        // }
    }

    override fun onCompletedForNode(node: TaskNode) {
//        if (node.task is TopTask) return
//        logI("Task-${node.taskName} Completed.")
    }
}