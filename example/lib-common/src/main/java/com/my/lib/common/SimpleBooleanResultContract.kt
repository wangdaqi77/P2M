package com.my.lib.common

import android.app.Activity
import android.content.Intent
import com.p2m.core.launcher.ActivityResultContractCompat

class SimpleBooleanResultContract: ActivityResultContractCompat<Unit, Boolean>() {
    companion object {
        fun makeSuccess(activity: Activity) {
            activity.setResult(Activity.RESULT_OK)
        }
    }

    override fun inputIntoCreatedIntent(input: Unit, intent: Intent) {
    }

    override fun outputFromResultIntent(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}