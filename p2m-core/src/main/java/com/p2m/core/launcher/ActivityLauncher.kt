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
import com.p2m.annotation.module.api.ApiLauncherActivityResultContractFor
import com.p2m.core.channel.Channel
import com.p2m.core.channel.InterruptibleChannel
import com.p2m.core.internal.launcher.InternalActivityLauncher
import com.p2m.core.internal.launcher.InternalSafeIntent
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
 * val fragment = P2M.apiOf(Account)
 *      .launcher
 *      .activityOfLogin
 *      .launchChannel(::startActivity)
 *      .navigation()
 * ```
 *
 * @see ApiLauncher
 */
interface ActivityLauncher<I, O> : Launcher{

    class Delegate<I, O>(clazz: Class<*>, createActivityResultContractBlock: () -> ActivityResultContractCompat<I, O>) {
        private val real by lazy(LazyThreadSafetyMode.NONE) {
            InternalActivityLauncher<I, O>(clazz, createActivityResultContractBlock)
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): ActivityLauncher<I, O> = real
    }

    /**
     * Launch for that [Activity] class annotated by [ApiLauncher],
     * all other fields (action, data, type) are null, though
     * they can be modified later in [launchBlock].
     */
    fun launchChannel(launchBlock: LaunchActivityBlock) : InterruptibleChannel

    /**
     * Register a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create, that type is implement class of
     * [ActivityResultContractCompat] and use [ApiLauncherActivityResultContractFor]
     * annotated, and that must has a empty constructor.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ApiLauncherActivityResultContractFor
     * @see ActivityResultContractCompat
     */
    fun registerResultLauncher(activity: ComponentActivity, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O>

    /**
     * Register a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create, that type is implement class of
     * [ActivityResultContractCompat] and use [ApiLauncherActivityResultContractFor]
     * annotated, and that must has a empty constructor.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ApiLauncherActivityResultContractFor
     * @see ActivityResultContractCompat
     */
    fun registerResultLauncher(fragment: Fragment, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O>

    /**
     * Register a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create, that type is implement class of
     * [ActivityResultContractCompat] and use [ApiLauncherActivityResultContractFor]
     * annotated, and that must has a empty constructor.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ApiLauncherActivityResultContractFor
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
     * Launch a activity result for that [Activity] class annotated by [ApiLauncher].
     *
     * No need to explicitly pass in a instance of activity result contract during registration,
     * that instance will auto create, that type is implement class of
     * [ActivityResultContractCompat] and use [ApiLauncherActivityResultContractFor]
     * annotated, and that must has a empty constructor.
     *
     * @return a instance of ActivityResultLauncher.
     *
     * @see ApiLauncher
     * @see ApiLauncherActivityResultContractFor
     * @see ActivityResultContractCompat
     */
    fun launchChannel(options: ActivityOptionsCompat? = null, inputBlock: () -> I) : InterruptibleChannel

    fun unregister()

    fun getContract(): ActivityResultContractCompat<I, O>
}


internal class InternalActivityResultLauncherCompat<I, O>(
    private val activityLauncher: ActivityLauncher<I, O>,
    private val activityResultLauncher: ActivityResultLauncher<I>
) : ActivityResultLauncherCompat<I, O> {

    override fun launchChannel(options: ActivityOptionsCompat?, inputBlock: () -> I) =
        Channel.interruptible(activityLauncher) {
            activityResultLauncher.launch(inputBlock(), options)
        }

    override fun unregister() = activityResultLauncher.unregister()

    @Suppress("UNCHECKED_CAST")
    override fun getContract(): ActivityResultContractCompat<I, O> =
        activityResultLauncher.contract as ActivityResultContractCompat<I, O>
}

abstract class ActivityResultContractCompat<I, O> :
    ActivityResultContract<I, ActivityResultCompat<O>>() {

    internal lateinit var activityClazz: Class<*>

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
        return intent.also { inputIntoCreatedIntent(input, it) }
    }

    final override fun parseResult(resultCode: Int, intent: Intent?): ActivityResultCompat<O> =
        ActivityResultCompat(resultCode, outputFromResultIntent(resultCode, intent))
}

class DefaultActivityResultContractCompat : ActivityResultContractCompat<Intent, Intent>() {

    override fun inputIntoCreatedIntent(input: Intent, intent: Intent) = Unit

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
