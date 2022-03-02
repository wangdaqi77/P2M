package com.p2m.example.account

import android.content.Intent
import com.p2m.core.launcher.ActivityResultContractCompat

class ModifyUserNameActivityResultContract: ActivityResultContractCompat<String, String>() {
    override fun inputIntoCreatedIntent(input: String, intent: Intent) {
        intent.putExtra("current_user_name", input)
    }

    override fun outputFromResultIntent(resultCode: Int, intent: Intent?): String? {
        return intent?.getStringExtra("result_new_user_name")
    }
}