package com.p2m.core.internal.channel

import android.content.Context
import com.p2m.core.channel.ChannelInterceptorFactory
import com.p2m.core.channel.IInterceptor
import com.p2m.core.exception.P2MException
import com.p2m.core.internal.log.logW
import com.p2m.core.launcher.GlobalLaunchActivityInterceptor
import com.p2m.core.launcher.ILaunchActivityInterceptor
import com.p2m.core.module.Module
import kotlin.reflect.KClass

internal class ChannelInterceptorContainer {
    private val factory: ChannelInterceptorFactory = DefaultChannelInterceptorFactory()
    private val _tableGlobalHeadForLaunchActivity = HashMap<GlobalLaunchActivityInterceptor, IInterceptor>()
    private val _tableGlobalTailForLaunchActivity = HashMap<GlobalLaunchActivityInterceptor, IInterceptor>()
    private val _globalHeadForLaunchActivity = ArrayList<IInterceptor>()
    private val _globalTailForLaunchActivity = ArrayList<IInterceptor>()
    private val tableForLaunchActivity = HashMap<KClass<out ILaunchActivityInterceptor>, IInterceptor>()
    private val tableForModule = HashMap<KClass<out Module<*>>, MutableList<IInterceptor>>()

    val globalHeadForLaunchActivity: Collection<IInterceptor>
        get() = _globalHeadForLaunchActivity
    val globalTailForLaunchActivity: Collection<IInterceptor>
        get() = _globalTailForLaunchActivity

    /**
     * register interceptor for launch activity
     */
    fun registerFromModule(
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

    fun registerGlobalHead(
        context: Context,
        launchActivityInterceptor: GlobalLaunchActivityInterceptor
    ): IInterceptor {
        val interceptor = _tableGlobalHeadForLaunchActivity[launchActivityInterceptor]
        if (interceptor != null) {
            logW("already registered for $launchActivityInterceptor of head")
            return interceptor
        }
        return ILaunchActivityInterceptor.delegate(launchActivityInterceptor)
            .also {
                it.init(context)
                _tableGlobalHeadForLaunchActivity[launchActivityInterceptor] = it
                _globalHeadForLaunchActivity.add(0, it)
            }
    }

    fun registerGlobalTail(
        context: Context,
        launchActivityInterceptor: GlobalLaunchActivityInterceptor
    ): IInterceptor {
        val interceptor = _tableGlobalTailForLaunchActivity[launchActivityInterceptor]
        if (interceptor != null) {
            logW("already registered for $launchActivityInterceptor of tail")
            return interceptor
        }
        return ILaunchActivityInterceptor.delegate(launchActivityInterceptor)
            .also {
                it.init(context)
                _tableGlobalTailForLaunchActivity[launchActivityInterceptor] = it
                _globalTailForLaunchActivity.add(it)
            }
    }

    /**
     * unregister global interceptor for launch activity
     */
    fun unregisterGlobal(launchActivityInterceptor: GlobalLaunchActivityInterceptor) {
        var removed = _tableGlobalHeadForLaunchActivity.remove(launchActivityInterceptor)
        if (removed == null) {
            removed = _tableGlobalHeadForLaunchActivity.remove(launchActivityInterceptor)
            if (removed == null) logW("no registered for $launchActivityInterceptor of after")
        }
    }

    fun get(interceptorClass: KClass<out ILaunchActivityInterceptor>): IInterceptor {
        return tableForLaunchActivity[interceptorClass]
            ?: throw P2MException("no register for ${interceptorClass.qualifiedName}, only class defined in `Api` area are supported.")
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