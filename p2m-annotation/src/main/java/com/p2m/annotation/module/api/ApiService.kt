package com.p2m.annotation.module.api

/**
 * A class uses this annotation will extract its all public member method for
 * generate a interface for service of `Api` area.
 *
 * Use `P2M.apiOf(${moduleName}::class.java).service` to get service instance,
 * that `moduleName` is defined in `settings.gradle`.
 *
 * The annotation can only be used once within same module scope.
 *
 * For example, has a feature for logout in `Account` module:
 * ```kotlin
 * @ApiService
 * class Service {
 *      fun logout() {
 *          ...
 *      }
 * }
 * ```
 *
 * then logout in external module:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .service
 *      .logout()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ApiService