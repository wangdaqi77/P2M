package com.p2m.core.internal.log

import com.p2m.core.internal._P2M
import com.p2m.core.log.Level

internal fun logI(msg: String){
    _P2M.configManager.logger?.log(Level.INFO, msg)
}

internal fun logD(msg: String){
    _P2M.configManager.logger?.log(Level.DEBUG, msg)
}

internal fun logW(msg: String){
    _P2M.configManager.logger?.log(Level.WARNING, msg)
}

internal fun logE(msg: String, throwable: Throwable? = null){
    _P2M.configManager.logger?.log(Level.ERROR, msg, throwable)
}