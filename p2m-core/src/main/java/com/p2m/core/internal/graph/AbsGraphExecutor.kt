package com.p2m.core.internal.graph

import com.p2m.core.internal.execution.BeginDirection
import com.p2m.core.internal.execution.Executor
import java.util.concurrent.*

internal abstract class AbsGraphExecutor<KEY, NODE : Node<NODE>, GRAPH : Graph<KEY, NODE>> constructor(
    private val direction: BeginDirection
) : Executor {

    private var ownerThread: Thread? = null
    private var quit = false
    protected abstract val graph: GRAPH
    abstract val messageQueue: BlockingQueue<Runnable>

    override fun loop() {
        ownerThread = Thread.currentThread()
        quit = false

        runGraph(graph) {
            onCompletedForGraph(graph)
        }

        while (true) {
            val runnable = messageQueue.take()
            runnable.run()
            if (runnable is ExitRunnable) {
                break
            }
        }

        quit = true
        ownerThread = null
    }

    override fun executeTask(runnable: Runnable) {
        check(!quit) { "Not execute task, exit already." }

        if (ownerThread === Thread.currentThread() && runnable !is ExitRunnable) {
            runnable.run()
            return
        }

        messageQueue.put(runnable)
    }

    override fun postTask(runnable: Runnable) {
        check(!quit) { "Not post task, exit already." }

        if (ownerThread === Thread.currentThread() && runnable !is ExitRunnable) {
            runnable.run()
            return
        }
        messageQueue.put(runnable)
    }

    override fun postTaskDelay(ms: Long, runnable: Runnable) {
        // nothing
    }

    override fun cancelTask(runnable: Runnable) {
        // nothing
    }

    override fun quitLoop(runnable: Runnable?) {
        postTask(object : ExitRunnable {
            override fun run() {
                runnable?.run()
            }
        })
    }

    private fun runGraph(graph: GRAPH, onComplete: () -> Unit) {
        val function = { stage: Stage<NODE> ->
            check(!stage.hasRing) {
                "Cannot be interdependent：" + stage.ringNodes!!
                    .map { "[${it.key.name}, ${it.value.name}]" }
                    .joinToString()
            }
            runStage(stage) onStageComplete@{
                val count = graph.stageCompletedCount.incrementAndGet()
                onCompletedForStage(stage)
                if (graph.stageSize == count) {
                    quitLoop(Runnable { onComplete() })
                }
            }
        }
        when (direction) {
            BeginDirection.HEAD -> graph.eachStageBeginFromHead(function)
            BeginDirection.TAIL -> graph.eachStageBeginFromTail(function)
        }
    }

    private fun runStage(stage: Stage<NODE>, onStageComplete: () -> Unit) {
        if (stage.isEmpty) return
        check(!stage.hasRing) { "Prohibit interdependence between nodes." }
        stage.nodes?.run {
            forEach { node ->
                runNode(node) onNodeComplete@{
                    val count = stage.completedCount.incrementAndGet()
                    onCompleted(node)
                    if ((stage.nodes?.size ?: 0) == count) {
                        onStageComplete()
                    }
                }
            }
        }
    }

    abstract fun runNode(node: NODE, onNodeComplete: () -> Unit)

    abstract fun onCompletedForGraph(graph: GRAPH)

    abstract fun onCompletedForStage(stage: Stage<NODE>)

    abstract fun onCompleted(node: NODE)

    private interface ExitRunnable : Runnable
}


