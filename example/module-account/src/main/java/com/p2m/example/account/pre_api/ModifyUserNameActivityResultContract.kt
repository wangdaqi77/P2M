package com.p2m.example.account.pre_api

import android.content.Intent
import com.p2m.core.launcher.ActivityResultContractCompat

class ModifyUserNameActivityResultContract: ActivityResultContractCompat<Unit?, String>() {
    override fun inputIntoCreatedIntent(input: Unit?, intent: Intent) {
        // NOTHING
    }

    override fun outputFromResultIntent(resultCode: Int, intent: Intent?): String? {
        return intent?.getStringExtra("result_new_user_name")
    }
}