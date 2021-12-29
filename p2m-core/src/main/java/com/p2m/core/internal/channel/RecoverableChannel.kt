package com.p2m.core.internal.channel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import com.p2m.core.P2M
import com.p2m.core.channel.RecoverableChannel
import com.p2m.core.internal.execution.TagRunnable
import com.p2m.core.internal.log.logW
import java.util.concurrent.atomic.AtomicInteger

internal object RecoverableChannelHelper : Application.ActivityLifecycleCallbacks {
    private const val EXT_ID = "Key_RecoverableChannelHelper_RecoverableChannel"
    private const val TIMEOUT = 10_000L
    private const val ID_NO_SET = 0
    private val idFactory = AtomicInteger(ID_NO_SET)
    private val table by lazy(LazyThreadSafetyMode.NONE) { SparseArray<RecoverableChannel>() }
    private val timeoutRunnable by lazy(LazyThreadSafetyMode.NONE) { SparseArray<TagRunnable>() }

    fun init(context: Context) {
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(this)
    }

    fun saveRecoverableChannel(intent: Intent, recoverableChannel: RecoverableChannel) {
        val id = idFactory.incrementAndGet()
        intent.putExtra(EXT_ID, id)
        table.put(id, recoverableChannel)
        P2M._executor.postTaskDelay(TIMEOUT, TagRunnable(id) {
            timeoutRunnable.remove(id)
            table.remove(id)
            logW("removed $id because the activity was not opened for too long.")
        }.also { timeoutRunnable.put(id, it) })
    }

    fun findRecoverableChannel(activity: Activity): RecoverableChannel? {
        activity.recoverableChannelIdIfFind { id ->
            return table[id]
        }
        return null
    }

    private inline fun Activity.recoverableChannelIdIfFind(onFound: (Int) -> Unit) {
        intent.getIntExtra(EXT_ID, ID_NO_SET)
            .takeIf { it != ID_NO_SET }
            ?.also(onFound)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.recoverableChannelIdIfFind { id ->
            timeoutRunnable[id]?.also(P2M._executor::removeTask)
        }
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        activity.recoverableChannelIdIfFind(table::remove)
    }
}