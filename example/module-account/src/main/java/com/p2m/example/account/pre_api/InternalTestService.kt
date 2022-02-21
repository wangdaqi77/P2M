package com.p2m.example.account.pre_api

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.p2m.annotation.module.api.ApiLauncher

@ApiLauncher("Test")
class InternalTestService : Service() {

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "InternalTestService onCreate()", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        Toast.makeText(this, "InternalTestService onDestroy()", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}