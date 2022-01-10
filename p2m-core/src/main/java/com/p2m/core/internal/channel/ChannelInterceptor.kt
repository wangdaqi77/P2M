package com.p2m.core.internal.channel

import com.p2m.core.channel.ChannelInterceptorFactory
import com.p2m.core.channel.IInterceptor
import com.p2m.core.exception.P2MException
import com.p2m.core.internal.log.logW
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.module.Module
import kotlin.reflect.KClass

internal class ChannelInterceptorContainer {
    private val factory: ChannelInterceptorFactory = DefaultChannelInterceptorFactory()
    private val tableForLaunchActivity = HashMap<KClass<out ILaunchActivityInterceptor>, IInterceptor>()
    private val tableForModule = HashMap<KClass<out Module<*>>, MutableList<IInterceptor>>()

    /**
     * for launch activity
     */
    fun register(
        module: Module<*>,
        launchActivityInterceptorClass: KClass<out ILaunchActivityInterceptor>,
        launchActivityInterceptor: ILaunchActivityInterceptor
    ): IInterceptor {
        val interceptor = tableForLaunchActivity[launchActivityInterceptorClass]
        if (interceptor != null) {
            logW("already registered for ${launchActivityInterceptorClass.qualifiedName}")
            return interceptor
        }
        return ILaunchActivityInterceptor.delegate(launchActivityInterceptor)
            .also {
                tableForLaunchActivity[launchActivityInterceptorClass] = it
                add(module, it)
            }
    }

    private fun add(module: Module<*>, interceptor: IInterceptor) {
        val list = tableForModule[module::class] ?: mutableListOf<IInterceptor>().also {
            tableForModule[module::class] = it
        }
        list.add(interceptor)
    }

    fun get(interceptorClass: KClass<out ILaunchActivityInterceptor>): IInterceptor {
        return tableForLaunchActivity[interceptorClass]
            ?: throw P2MException("no register for ${interceptorClass.qualifiedName}")
    }

    fun getInterceptors(module: Module<*>): List<IInterceptor>? = tableForModule[module::class]
}

internal class DefaultChannelInterceptorFactory : ChannelInterceptorFactory {
    override fun createInterceptor(interceptorClass: KClass<IInterceptor>): IInterceptor {
        return interceptorClass.java.newInstance()
    }

    override fun createLaunchActivityInterceptor(interceptorClass: KClass<ILaunchActivityInterceptor>): IInterceptor {
        return ILaunchActivityInterceptor.delegate(interceptorClass.java.newInstance())
    }
}