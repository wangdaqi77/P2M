package com.p2m.annotation.module

/**
 * A class uses this annotation for a module initialization of `Module init` area.
 *
 * Use `P2M.init()` to start to initialize all modules.
 *
 * The annotation can only be used once within same module scope.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Interceptor(
    val type: Type,
) {
    enum class Type {
        GLOBAL_BEFORE,
        GLOBAL_AFTER,
        LAUNCHER,
    }
}


