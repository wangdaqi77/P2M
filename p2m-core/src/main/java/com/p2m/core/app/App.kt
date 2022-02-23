package com.p2m.core.app

import com.p2m.core.internal.app.AppModuleInit
import com.p2m.core.module.*

class App: Module<AppModuleApi>() {
    override val init: ModuleInit = AppModuleInit()
    override val api: AppModuleApi = AppModuleApi()
    override val publicClass: Class<out Module<*>> = App::class.java
    internal var onEvaluateTooLongStart: (() -> Unit)? = null
    internal var onEvaluateTooLongEnd: (() -> Unit)? = null

    internal fun onEvaluate(onEvaluate:() -> Unit):App {
        (init as AppModuleInit).onEvaluate = onEvaluate
        return this
    }

    internal fun onCompleted(onCompleted:() -> Unit):App {
        (init as AppModuleInit).onCompleted = onCompleted
        return this
    }

    internal fun onEvaluateTooLongStart(onEvaluateTooLongStart:() -> Unit):App {
        this.onEvaluateTooLongStart = onEvaluateTooLongStart
        return this
    }

    internal fun onEvaluateTooLongEnd(onEvaluateTooLongEnd:() -> Unit):App {
        this.onEvaluateTooLongEnd = onEvaluateTooLongEnd
        return this
    }
}