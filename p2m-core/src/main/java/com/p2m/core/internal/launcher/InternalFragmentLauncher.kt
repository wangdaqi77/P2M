package com.p2m.core.internal.launcher

import com.p2m.core.launcher.FragmentLauncher
import com.p2m.core.launcher.LaunchChannel
import com.p2m.core.launcher.LaunchFragmentBlock

internal class InternalFragmentLauncher<T>(private val createBlock: () -> T) : FragmentLauncher<T> {
    override fun launchChannel(launchBlock: LaunchFragmentBlock<T>) =
        LaunchChannel.delegate(this) {
            launchBlock(createBlock())
        }
}