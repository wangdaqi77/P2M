package com.p2m.core.internal.module

import android.content.Context
import com.p2m.core.internal.graph.Graph
import com.p2m.core.internal.log.logW
import com.p2m.core.internal.module.task.DefaultTaskFactory
import com.p2m.core.module.Module
import com.p2m.core.module.task.TaskFactory

internal class ModuleGraph private constructor(
    private val context: Context,
    private val moduleContainer: ModuleContainerDefault,
    private val top: Module<*>
) : Graph<Class<out Module<*>>, ModuleNode>() {

    companion object{
        internal fun create(context:Context, moduleContainer: ModuleContainerDefault, top: Module<*>): ModuleGraph {
            return ModuleGraph(context, moduleContainer, top)
        }
    }

    private val taskFactory: TaskFactory = DefaultTaskFactory()
    private val nodes: HashMap<Class<out Module<*>>, ModuleNode> = HashMap()
    val moduleSize
        get() = moduleContainer.getAll().size

    override fun evaluate():HashMap<Class<out Module<*>>, ModuleNode>{
        reset()
        createNodes()
        layout()
        return nodes
    }

    private fun Class<out Module<*>>.dependOn(dependClass: Class<out Module<*>>) {
        val ownerNode = nodes[this] ?: return
        val node = nodes[dependClass] ?: return

        if (node.byDependNodes.add(ownerNode)) {
            node.byDependDegree++
        }

        if (ownerNode.dependNodes.add(node)) {
            ownerNode.dependDegree++
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
    }

    private fun createNodes() {
        createNodeRecursive(top, true)
    }

    private fun createNodeRecursive(module: Module<*>, isTop: Boolean) {
        val moduleUnit = module.internalModuleUnit
        nodes[moduleUnit.moduleImplClass] = ModuleNode(
            context,
            module,
            isTop,
            taskFactory
        )

        for (dependency in moduleUnit.getDependencies()) {
            val moduleDependency = moduleContainer.find(dependency)!!
            createNodeRecursive(moduleDependency, false)
        }
    }
    
    private fun depends() {
        for (node in nodes) {
            val owner = node.key
            moduleContainer.find(owner)?.run {
                for (dependency in internalModuleUnit.getDependencies()) {
                    if (!nodes.containsKey(dependency)) {
                        logW("${owner.canonicalName} depend on ${dependency.canonicalName}, but not registered of ${dependency.canonicalName}")
                    }else {
                        owner.dependOn(dependency)
                    }
                }
            }
        }
    }

}