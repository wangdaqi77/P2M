package com.p2m.core.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.channel.IInterceptor
import com.p2m.core.internal._P2M
import com.p2m.core.internal.launcher.InternalActivityLauncher
import com.p2m.core.internal.launcher.InternalSafeIntent
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A launcher of `Activity`.
 *
 * For example, has a `Activity` of login in `Account` module:
 * ```kotlin
 * @ApiLauncher("Login")
 * class LoginActivity:Activity()
 * ```
 *
 * then launch in `activity` of external module:
 * ```kotlin
 * P2M.apiOf(Account)
 *      .launcher
 *      .activityOfLogin
 *      .launchChannel(::startActivity)
 *      .navigation()
 * ```
 *
 * @see ApiLauncher
 */
interface ActivityLauncher<I, O> : Launcher{
    companion object {
        fun <I, O> delegate(
            clazz: Class<*>,
            vararg annotatedInterceptorClass: KClass<out ILaunchActivityInterceptor>,
            createActivityResultContractBlock: () -> ActivityResultContractCompat<I, O>
        ) : Lazy<ActivityLauncher<I, O>> = lazy(LazyThreadSafetyMode.NONE) {
            InternalActivityLauncher(
                clazz,
                annotatedInterceptorClass,
                createActivityResultContractBlock
            )
        }
    }

    /**
     * Will create a interruptible launch channel for that [Activity] class
     * uses annotation [ApiLauncher], please call `navigation` to launch.
     *
     * [launchBlock] is real launch method, that has a created Intent instance
     * as input param, all other fields (action, data, type) are null, they can
     * be modified later in [launchBlock].
     *
     * @return [LaunchActivityChannel] - call `navigation` to launch.
     *
     * @see ApiLauncher
     * @see LaunchActivityChannel.navigation
     * @see IInterceptor
     */
    fun launchChannel(launchBlock: LaunchActivityBlock) : LaunchActivityChannel

    /**
     * Register a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ActivityResultContractCompat
     */
    fun registerResultLauncher(activity: ComponentActivity, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O>

    /**
     * Register a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ActivityResultContractCompat
     */
    fun registerResultLauncher(fragment: Fragment, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O>

    /**
     * Register a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ActivityResultContractCompat
     */
    fun registerResultLauncher(activityResultRegistry: ActivityResultRegistry, key: String, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O>
}

/**
 * A launcher of Activity Result.
 *
 * @see ApiLauncher
 */
interface ActivityResultLauncherCompat<I, O> {
    /**
     * Will create a interruptible launch channel for that [Activity] class
     * uses annotation [ApiLauncher], please call `navigation` to launch.
     *
     * Will launch activity result if there is no interruption, and at same
     * time call [inputBlock] get input as launch input param.
     *
     * @return [LaunchActivityChannel] - call `navigation` to launch.
     *
     * @see ActivityLauncher.registerResultLauncher
     * @see LaunchActivityChannel.navigation
     * @see IInterceptor
     */
    fun launchChannel(options: ActivityOptionsCompat? = null, inputBlock: () -> I) : LaunchActivityChannel

    fun unregister()

    fun getContract(): ActivityResultContractCompat<I, O>
}


internal class InternalActivityResultLauncherCompat<I, O>(
    private val activityLauncher: InternalActivityLauncher<I, O>,
    private val activityResultLauncher: ActivityResultLauncher<I>
) : ActivityResultLauncherCompat<I, O> {

    override fun launchChannel(options: ActivityOptionsCompat?, inputBlock: () -> I) =
        LaunchActivityChannel.create(activityLauncher) { channel ->
            getContract().onCreateIntent = { intent: Intent ->
                // after `activityResultLauncher.launch(inputBlock(), options)`
                _P2M.onLaunchActivityNavigationCompletedBefore(channel, intent)
            }
            activityResultLauncher.launch(inputBlock(), options)
        }.also(activityLauncher::addAnnotatedInterceptor)

    override fun unregister() = activityResultLauncher.unregister()

    @Suppress("UNCHECKED_CAST")
    override fun getContract(): ActivityResultContractCompat<I, O> =
        activityResultLauncher.contract as ActivityResultContractCompat<I, O>
}

abstract class ActivityResultContractCompat<I, O> :
    ActivityResultContract<I, ActivityResultCompat<O>>() {

    internal lateinit var activityClazz: Class<*>
    internal var onCreateIntent: ((Intent) -> Unit)? = null

    /**
     * Fill input into created intent.
     *
     * @param input - from input of [ActivityResultLauncher.launch].
     * @param intent - from returns of [createIntent].
     */
    abstract fun inputIntoCreatedIntent(input: I, intent: Intent)

    /**
     * Returns output of result, that will provide to [ActivityResultCallbackCompat].
     *
     * @param resultCode - from [Activity.setResult] of owner activity.
     * @param intent - from [Activity.setResult] of owner activity.
     * @return - output of result.
     *
     * @see ActivityLauncher.registerResultLauncher
     * @see InternalActivityResultLauncherCompat.launchChannel
     */
    abstract fun outputFromResultIntent(resultCode: Int, intent: Intent?): O?

    final override fun createIntent(context: Context, input: I): Intent {
        val intent = if (input is Intent) InternalSafeIntent(input as Intent) else InternalSafeIntent()
        intent.setClassInternal(activityClazz)
        onCreateIntent?.invoke(intent)
        return intent.also { inputIntoCreatedIntent(input, it) }
    }

    final override fun parseResult(resultCode: Int, intent: Intent?): ActivityResultCompat<O> =
        ActivityResultCompat(resultCode, outputFromResultIntent(resultCode, intent))
}

class DefaultActivityResultContractCompat : ActivityResultContractCompat<Unit, Intent>() {

    override fun inputIntoCreatedIntent(input: Unit, intent: Intent) = Unit

    override fun outputFromResultIntent(resultCode: Int, intent: Intent?): Intent? = intent
}

data class ActivityResultCompat<O>(val resultCode: Int, val output: O?)

/**
 * A callback for receive activity result.
 */
typealias ActivityResultCallbackCompat<O> = (resultCode: Int, output: O?) -> Unit

/**
 * A block for launch service.
 */
typealias LaunchActivityBlock = (createdIntent: Intent) -> Unit
