package com.p2m.core.launcher

import com.p2m.core.channel.ChannelBlock
import com.p2m.core.channel.GreenChannel


class LaunchServiceChannel internal constructor(
    val launcher: Launcher,
    channelBlock: ChannelBlock
) : GreenChannel(launcher, channelBlock) {
    companion object {
        internal fun create(launcher: Launcher, channelBlock: ChannelBlock) =
            LaunchServiceChannel(launcher, channelBlock)
    }
}
