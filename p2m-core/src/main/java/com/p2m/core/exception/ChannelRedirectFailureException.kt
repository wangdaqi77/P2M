package com.p2m.core.exception

import com.p2m.core.channel.Channel

class ChannelRedirectFailureException(val redirectChannel: Channel, message:String, cause: Throwable? = null): Exception(message, cause)