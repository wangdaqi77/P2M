package com.p2m.core.exception

import com.p2m.core.channel.ChannelRedirectionMode
import com.p2m.core.channel.RecoverableChannel

class ChannelRedirectInterruptedException constructor(val mode: ChannelRedirectionMode, val recoverableChannel: RecoverableChannel): Exception()