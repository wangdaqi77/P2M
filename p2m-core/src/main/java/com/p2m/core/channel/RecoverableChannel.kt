package com.p2m.core.channel

class RecoverableChannel internal constructor(val channel: InterruptibleChannel) {
    fun navigation() = channel.navigation()
}