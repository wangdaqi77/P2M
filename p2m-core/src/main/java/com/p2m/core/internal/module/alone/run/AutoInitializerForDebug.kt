package com.p2m.core.internal.module.alone.run

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.p2m.core.P2M
import com.p2m.core.log.ILogger
import com.p2m.core.log.Level


internal class AutoInitializerForDebug : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false

        P2M.config {
            logger = object : ILogger {
                override fun log(level: Level, msg: String, throwable: Throwable?) {
                    when(level) {
                        Level.INFO -> Log.i("P2M", "AutoInitializer -> $msg", throwable)
                        Level.DEBUG -> Log.d("P2M", "AutoInitializer -> $msg", throwable)
                        Level.WARNING -> Log.w("P2M", "AutoInitializer -> $msg", throwable)
                        Level.ERROR -> Log.e("P2M", "AutoInitializer -> $msg", throwable)
                    }
                }
            }
        }

        P2M.init(context)
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor?  = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = -1

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = -1
    override fun getType(uri: Uri): String? = null
}