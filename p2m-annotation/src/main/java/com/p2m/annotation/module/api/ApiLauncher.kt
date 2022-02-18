package com.p2m.annotation.module.api

import com.p2m.core.launcher.*
import kotlin.reflect.KClass

/**
 * A class uses this annotation will generate a launch property for launcher of `Api` area.
 *
 * Use `P2M.apiOf(${moduleName}::class.java).launcher.${generatedProperty}` to get `launcher` instance,
 * that `moduleName` is defined in `settings.gradle`.
 *
 * Supports:
 *  * Activity - will generate a property for launch activity,
 *  that property name is `activityOf$launcherName`, also can use
 *  [activityResultContract] specify a result contract, also can
 *  use [launchActivityInterceptor] specify some interceptors.
 *  * Fragment - will generate a property for create fragment,
 *  that property name is `fragmentOf$launcherName`.
 *  * Service  - will generate a property for launch service,
 *  that property name is `serviceOf$launcherName`.
 *
 * For example, has a activity for login in `Account` module:
 * ```kotlin
 * @ApiLauncher("Login")
 * class LoginActivity : Activity()
 * ```
 *
 * then launch:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .launcher
 *      .activityOfLogin
 *      .launchChannel(::startActivity)
 *      .navigation()
 * ```
 *
 * @property launcherName used to generate property names, it follows the hump nomenclature.
 * @property activityResultContract specify a activity result contract for activity.
 * @property launchActivityInterceptor specify interceptors for activity.
 *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ApiLauncher(val launcherName: String, val activityResultContract: KClass<out ActivityResultContractCompat<*,*>> = DefaultActivityResultContractCompat::class, vararg val launchActivityInterceptor: KClass<out ILaunchActivityInterceptor>) {
    companion object{
        private val NAME_REGEX = Regex( "^[A-Z][A-Za-z0-9]*$")

        fun checkName(launch: ApiLauncher, clazzName: String){
            check(launch.launcherName.isEmpty() || launch.launcherName.matches(NAME_REGEX)) {
                "The ApiLauncher(name = \"${launch.launcherName}\") at $clazzName, that name must matches ${NAME_REGEX.pattern}"
            }
        }
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class LaunchActivityInterceptor(val interceptorName: String) {
    companion object {
        private val NAME_REGEX = Regex("^[A-Z][A-Za-z0-9]*$")
        fun checkName(interceptor: LaunchActivityInterceptor, clazzName: String) {
            check(interceptor.interceptorName.isEmpty() || interceptor.interceptorName.matches(NAME_REGEX)) {
                "The LaunchActivityInterceptor(name = \"${interceptor.interceptorName}\") at $clazzName, that name must matches ${NAME_REGEX.pattern}"
            }
        }
    }
}