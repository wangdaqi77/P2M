package com.p2m.annotation.module

import com.p2m.annotation.module.api.ApiUse

/**
 * A class uses this annotation for a module initialization of `Module init` area.
 *
 * Use `P2M.init()` to start to initialize all modules.
 *
 * The annotation can only be used once within same module scope.
 */
@ApiUse
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ActivityInterceptor


