package com.p2m.example.mall

import android.app.Application
import android.util.Log
import com.p2m.core.P2M
import com.p2m.core.log.ILogger
import com.p2m.core.log.Level

class MallApp:Application() {
    override fun onCreate() {
        super.onCreate()
        P2M.config {
            logger = object : ILogger {
                override fun log(level: Level, msg: String, throwable: Throwable?) {
                    when(level) {
                        Level.INFO -> Log.i("P2M", msg, throwable)
                        Level.DEBUG -> Log.d("P2M", msg, throwable)
                        Level.WARNING -> Log.w("P2M", msg, throwable)
                        Level.ERROR -> Log.e("P2M", msg, throwable)
                    }
                }
            }
        }
        P2M.init(this)
    }
}