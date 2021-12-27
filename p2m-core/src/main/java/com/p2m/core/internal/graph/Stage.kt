package com.p2m.core.internal.graph

import java.lang.StringBuilder
import java.util.concurrent.atomic.AtomicInteger

internal class Stage<N : Node<N>> {

    var nodes: ArrayList<N>? = null
        set(value) {
            updateName(value)
            field = value
        }

    var hasRing = false

    var ringNodes: Map<N, N>? = null

    var completedCount = AtomicInteger()

    var name: String = "UNSET"

    val isEmpty
        get() = nodes == null || nodes!!.isEmpty()

    private fun updateName(value: ArrayList<N>?) {
        val size = value?.size ?: 0
        name = if (size > 0) {
            val sb = StringBuilder(size * 10)
            sb.append("[")
            value?.forEachIndexed { index, n ->
                if (index == size - 1) {
                    sb.append(n.name)
                } else {
                    sb.append("${n.name}, ")
                }
            }
            sb.append("]")
            sb.toString()
        } else {
            "[]"
        }
    }
}