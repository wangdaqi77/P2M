package com.p2m.core.launcher

interface Launcher

class LaunchChannelDelegate(launcher: Launcher, channel: Channel) :
    LaunchChannel(launcher, channel) {

    override fun timeout(timeout: Long): LaunchChannelDelegate {
        return super.timeout(timeout) as LaunchChannelDelegate
    }

    fun onFailure(failureBlock: (channel: LaunchChannel) -> Unit): LaunchChannelDelegate {
        return super.onFailure{ failureBlock(it as LaunchChannel) } as LaunchChannelDelegate
    }

    fun onIntercept(interceptBlock: (block: LaunchChannel) -> Unit): LaunchChannelDelegate {
        return super.onIntercept { interceptBlock(it as LaunchChannel) } as LaunchChannelDelegate
    }

    fun launch() {
        invoke()
    }
}

abstract class LaunchChannel(val launcher: Launcher, channel: Channel) :
    InterceptableChannel(launcher, channel) {
    companion object {
        fun delegate(launcher: Launcher, channel: Channel) = LaunchChannelDelegate(launcher, channel)
    }
}

val Launcher.isActivityLauncher: Boolean
    inline get() = this is ActivityLauncher<*, *>

inline val Launcher.isFragmentLauncher: Boolean
    get() = this is FragmentLauncher<*>

inline val Launcher.isServiceLauncher: Boolean
    get() = this is ServiceLauncher