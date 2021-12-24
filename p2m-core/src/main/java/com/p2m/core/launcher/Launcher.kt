package com.p2m.core.launcher

interface Launcher

val Launcher.isActivityLauncher: Boolean
    inline get() = this is ActivityLauncher<*, *>

val Launcher.isFragmentLauncher: Boolean
    get() = this is FragmentLauncher<*>

val Launcher.isServiceLauncher: Boolean
    get() = this is ServiceLauncher

class LaunchChannel()