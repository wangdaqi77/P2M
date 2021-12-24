package com.p2m.core.internal.launcher

import com.p2m.core.launcher.FragmentLauncher

internal class InternalFragmentLauncher<T>(private val createBlock: () -> T) : FragmentLauncher<T> {
    override fun navigation() = createBlock()
}