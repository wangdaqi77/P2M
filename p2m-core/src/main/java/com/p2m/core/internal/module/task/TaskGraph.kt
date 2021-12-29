package com.p2m.core.internal.module.task

import android.content.Context
import com.p2m.core.internal.graph.Graph
import com.p2m.core.internal.log.logW
import com.p2m.core.internal.module.SafeModuleApiProvider
import com.p2m.core.module.task.Task

internal class TaskGraph private constructor(
    val context: Context,
    val moduleName: String,
    private val taskContainer: TaskContainerImpl,
    val safeModuleApiProvider: SafeModuleApiProvider
) : Graph<Class<out Task<*, *>>, TaskNode>() {
    private val nodes: HashMap<Class<out Task<*, *>>, TaskNode> = HashMap()
    val taskSize
        get() = taskContainer.getAll().size

    companion object{
        internal fun create(context: Context, moduleName: String, taskContainer: TaskContainerImpl, SafeModuleApiProvider: SafeModuleApiProvider): TaskGraph {
            return TaskGraph(context, moduleName, taskContainer, SafeModuleApiProvider)
        }
    }

    private fun Class<out Task<*, *>>.dependOn(dependClass: Class<out Task<*, *>>) {
        val ownerNode = nodes[this] ?: return
        val node = nodes[dependClass] ?: return

        if (node.byDependNodes.add(ownerNode)) {
            node.byDependDegree++
        }

        if (ownerNode.dependNodes.add(node)) {
            ownerNode.dependDegree++
        }
    }

    override fun evaluate(): HashMap<Class<out Task<*, *>>, TaskNode> {
        reset()
        createNodes()
        layout()
        return nodes
    }
    
    private fun dependsForTop(){
        val topClass = taskContainer.topTaskClass
        nodes
            .filter {
                it.value.byDependDegree == 0 && it.key !== topClass
            }
            .keys.forEach {
                topClass.dependOn(it)
            }
    }

    private fun reset(){
        resetNode()
        resetStage()
    }

    private fun resetNode(){
        nodes.clear()
    }

    private fun resetStage(){
        stageSize = 0
        stageCompletedCount.set(0)
    }

    
    private fun layout(){
        depends()
        dependsForTop()
    }
    
    private fun createNodes() {
        taskContainer.getAll().forEach {
            val clazz = it.getOwnerClass()
            val taskProvider = TaskOutputProviderImplForTask(taskContainer, it)
            nodes[clazz] = TaskNode(
                context = context,
                taskName = "${moduleName}::${clazz.simpleName}",
                task = it.ownerInstance,
                input = it.input,
                taskProvider = taskProvider,
                clazz === taskContainer.topTaskClass
            )
        }
    }
    
    private fun depends() {
        taskContainer.getAll().forEach {
            val owner = it.getOwnerClass()
            it.getDependencies().forEach { dependClass ->
                if (!nodes.containsKey(dependClass)) logW("${owner.canonicalName} depend on ${dependClass.canonicalName}, but not registered of ${dependClass.canonicalName}")
                owner.dependOn(dependClass)
            }
        }
    }

}