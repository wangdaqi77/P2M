package com.p2m.annotation.module

/**
 * A class uses this annotation for a module initialization.
 *
 * Use `P2M.init()` to start initialize for all modules.
 *
 * The annotation can only be used once within same module scope.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ModuleInitializer