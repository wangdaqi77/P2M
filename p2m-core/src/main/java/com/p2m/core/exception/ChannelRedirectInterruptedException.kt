package com.p2m.core.exception

import com.p2m.core.channel.Channel

class ChannelRedirectInterruptedException(val redirectChannel: Channel, message:String, cause: Throwable? = null): Exception(message, cause)