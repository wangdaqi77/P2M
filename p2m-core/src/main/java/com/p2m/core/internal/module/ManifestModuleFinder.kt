package com.p2m.core.internal.module

import android.content.Context
import android.content.pm.PackageManager
import com.p2m.core.exception.P2MException
import com.p2m.core.module.Module
import java.lang.RuntimeException

internal class ManifestModuleFinder(context: Context) {

    private val table = HashMap<String, Class<out Module<*>>>()
    private val metaData =
        context
            .packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData

    @Suppress("UNCHECKED_CAST")
    fun getModuleImplClass(moduleName: String): Class<out Module<*>> {
        if (table.containsKey(moduleName)) {
            return table[moduleName]!!
        }
        val value = metaData.getString("p2m:module=${moduleName}")
            ?: throw P2MException("Not found module impl class, that name is $moduleName")
        val attributes = value.trim().split(",")
        attributes.forEach {
            val attribute = it.trim().split("=")
            // implModuleClass
            // publicModuleClass
            val key = attribute[0].trim()
            val value = attribute[1].trim()
            if (key == "implModuleClass") {
                val clazz = Class.forName(value) as Class<out Module<*>>
                table[moduleName] = clazz
                return clazz
            }
        }
        throw P2MException("Not found module impl class, that name is $moduleName")
    }
}