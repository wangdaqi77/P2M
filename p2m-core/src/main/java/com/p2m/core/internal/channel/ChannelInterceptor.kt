package com.p2m.core.internal.channel

import com.p2m.core.channel.ChannelInterceptorFactory
import com.p2m.core.channel.IInterceptor
import com.p2m.core.exception.P2MException
import com.p2m.core.launcher.ILaunchActivityInterceptor
import kotlin.reflect.KClass

internal class ChannelInterceptorContainer {
    private val factory: ChannelInterceptorFactory = DefaultChannelInterceptorFactory()
    private val tableForLaunchActivity = HashMap<KClass<ILaunchActivityInterceptor>, IInterceptor>()

    fun register(interceptorClass: KClass<ILaunchActivityInterceptor>): IInterceptor {
        return tableForLaunchActivity[interceptorClass] ?: factory.createLaunchActivityInterceptor(interceptorClass)
            .also { tableForLaunchActivity[interceptorClass] = it }
    }

    fun get(interceptorClass: KClass<ILaunchActivityInterceptor>): IInterceptor {
        return tableForLaunchActivity[interceptorClass]
            ?: throw P2MException("no register for ${interceptorClass.qualifiedName}")
    }
}

internal class DefaultChannelInterceptorFactory : ChannelInterceptorFactory {
    override fun createInterceptor(interceptorClass: KClass<IInterceptor>): IInterceptor {
        return interceptorClass.java.newInstance()
    }

    override fun createLaunchActivityInterceptor(interceptorClass: KClass<ILaunchActivityInterceptor>): IInterceptor {
        return ILaunchActivityInterceptor.delegate(interceptorClass.java.newInstance())
    }
}