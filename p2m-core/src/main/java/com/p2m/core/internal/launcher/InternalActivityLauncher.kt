package com.p2m.core.internal.launcher

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.p2m.annotation.module.api.ApiLauncher
import com.p2m.core.launcher.LaunchActivityChannel
import com.p2m.core.internal._P2M
import com.p2m.core.launcher.*
import kotlin.reflect.KClass

internal class InternalActivityLauncher<I, O>(
    private val clazz: Class<*>,
    private val annotatedInterceptorClasses: Array<out KClass<out ILaunchActivityInterceptor>>,
    private val createActivityResultContractBlock: () -> ActivityResultContractCompat<I, O>
) : ActivityLauncher<I, O> {

    /**
     * Create a instance of Intent for that [Activity] class annotated by [ApiLauncher],
     * all other fields (action, data, type) are null, though they can be modified
     * later with explicit calls.
     *
     * @return a instance of Intent.
     */
    private fun createIntent(): InternalSafeIntent {
        return InternalSafeIntent(clazz)
    }

    override fun launchChannel(launchBlock: LaunchActivityBlock): LaunchActivityChannel {
        val intent = createIntent()
        return LaunchActivityChannel.create(this) { channel ->
            _P2M.onLaunchActivityNavigationCompletedBefore(channel, intent)
            launchBlock(intent)
        }.also(::addAnnotatedInterceptor)
    }

    override fun registerResultLauncher(activity: ComponentActivity, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O> {
        return activity.registerForActivityResult(createActivityResultContract()) {
            callback.invoke(it.resultCode, it.output)
        }.compat()
    }

    override fun registerResultLauncher(fragment: Fragment, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O> {
        return fragment.registerForActivityResult(createActivityResultContract()) {
            callback.invoke(it.resultCode, it.output)
        }.compat()
    }

    override fun registerResultLauncher(activityResultRegistry: ActivityResultRegistry, key: String, callback: ActivityResultCallbackCompat<O>): ActivityResultLauncherCompat<I, O> {
        return activityResultRegistry.register(key, createActivityResultContract()) {
            callback.invoke(it.resultCode, it.output)
        }.compat()
    }

    private fun createActivityResultContract(): ActivityResultContractCompat<I, O> {
        return createActivityResultContractBlock.invoke().also { it.activityClazz = clazz }
    }

    internal fun addAnnotatedInterceptor(channel: LaunchActivityChannel){
        annotatedInterceptorClasses.forEach{
            channel.addInterceptorAfter(it)
        }
    }

    private fun ActivityResultLauncher<I>.compat(): InternalActivityResultLauncherCompat<I, O> =
        InternalActivityResultLauncherCompat(this@InternalActivityLauncher, this)

}