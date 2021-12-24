package com.p2m.core.internal.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.p2m.core.P2M
import com.p2m.core.exception.P2MException

internal class InternalSafeIntent : Intent {

    constructor() : super()

    constructor(o: Intent) : super(o)

    constructor(cls: Class<*>) : super(P2M.internalContext, cls)

    override fun setComponent(component: ComponentName?): Intent =
        throw P2MException("Immutable!")

    override fun setClassName(packageName: String, className: String): Intent =
        throw P2MException("Immutable!")

    override fun setClassName(packageContext: Context, className: String): Intent =
        throw P2MException("Immutable!")

    override fun setClass(packageContext: Context, cls: Class<*>): Intent =
        throw P2MException("Immutable!")

    fun setClassInternal(cls: Class<*>): Intent = super.setClass(P2M.internalContext, cls)
}