package com.p2m.core.internal.module

import com.p2m.core.app.App
import com.p2m.core.internal.execution.BeginDirection
import com.p2m.core.internal.execution.TagRunnable
import com.p2m.core.internal.graph.AbsGraphExecutor
import com.p2m.core.internal.graph.GraphThreadPoolExecutor
import com.p2m.core.internal.graph.Stage
import com.p2m.core.internal.graph.Node.State
import com.p2m.core.internal.log.logI
import com.p2m.core.internal.module.task.TaskOutputProviderImplForModule
import com.p2m.core.internal.module.task.TaskGraph
import com.p2m.core.internal.module.task.TaskGraphExecutor
import com.p2m.core.module.Module
import java.util.concurrent.*

internal class ModuleGraphExecutor(
    override val graph: ModuleGraph,
    private val isEvaluating: ThreadLocal<Boolean>,
    private val executingModuleProvider: ThreadLocal<SafeModuleApiProvider>,
    direction: BeginDirection
) : AbsGraphExecutor<Class<out Module<*>>, ModuleNode, ModuleGraph>(direction) {

    private val executor: ExecutorService = GraphThreadPoolExecutor()
    override val messageQueue: BlockingQueue<Runnable> = ArrayBlockingQueue(graph.moduleSize)

    override fun runNode(node: ModuleNode, onDependsNodeComplete: () -> Unit) {
        node.markStarted(onDependsNodeComplete) {
            // Started
            executor.execute(TagRunnable(node.name) {
                val evaluatingWhenDependingIdle = {
                    // Evaluating
                    node.evaluating()
                }

                // Depending
                node.depending(evaluatingWhenDependingIdle)

                // Executing
                node.executing()
                executeTask {
                    // Completed
                    node.markSelfAvailable()
                    node.executed()
                    onDependsNodeComplete()
                }
            })
        }
    }

    private fun ModuleNode.executing() {
        mark(State.EXECUTING)
        if (!taskContainer.onlyHasTop) {
            val taskGraph = TaskGraph.create(context, name, taskContainer, safeModuleApiProvider)
            val taskGraphExecution = TaskGraphExecutor(taskGraph, executor, executingModuleProvider, BeginDirection.TAIL)
            taskGraphExecution.loop()
        }
        module.internalInit.onExecute(context)
    }

    private fun ModuleNode.evaluating() {
        mark(State.EVALUATING)
        logI("$name `onEvaluate()`")
        isEvaluating.set(true)
        module.internalInit.onEvaluate(context, taskContainer)
        isEvaluating.set(false)
    }

    private fun ModuleNode.depending(evaluatingWhenDependingIdle: () -> Unit) {
        mark(State.DEPENDING)

        var idleRunning = true
        if (dependNodes.isEmpty()) {
            evaluatingWhenDependingIdle()
            idleRunning = false
            return
        }
        val countDownLatch = CountDownLatch(dependNodes.size)
        val onDependenciesComplete = {
            // Dependencies be Completed.
            countDownLatch.countDown()

            if (isTop && countDownLatch.count == 0L) {
                logI("all module completed.")

                if (idleRunning) {
                    (module as App).onEvaluateTooLongStart?.invoke()
                }
            }
        }

        dependNodes.forEach { dependNode ->
            runNode(dependNode, onDependenciesComplete)
        }

        if (isTop) {
            evaluatingWhenDependingIdle()
            if (countDownLatch.count == 0L) {
                (module as App).onEvaluateTooLongEnd?.invoke()
            }
        } else {
            evaluatingWhenDependingIdle()
        }
        idleRunning = false

        // Wait dependencies be Completed.
        countDownLatch.await()
    }

    private fun ModuleNode.executed() {
        logI("$name `onExecuted()`")
        val taskOutputProviderImplForModule = TaskOutputProviderImplForModule(taskContainer)

        executingModuleProvider.set(safeModuleApiProvider)
        module.internalInit.onExecuted(context, taskOutputProviderImplForModule)
        executingModuleProvider.set(null)
        markCompleted()
    }

    override fun onCompletedForGraph(graph: ModuleGraph) {
        logI("Module-Graph Completed.")
    }
    
    override fun onCompletedForStage(stage: Stage<ModuleNode>) {
        // logI("Module-Graph-Stage${stage.name} Completed.")
    }

    override fun onCompletedForNode(node: ModuleNode) {
//        logI("${node.name} Completed.")
    }

}