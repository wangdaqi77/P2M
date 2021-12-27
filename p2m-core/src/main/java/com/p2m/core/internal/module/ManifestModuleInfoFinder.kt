package com.p2m.core.internal.module

import android.content.Context
import android.content.pm.PackageManager
import com.p2m.core.exception.P2MException
import com.p2m.core.internal.log.logI
import com.p2m.core.internal.log.logW
import com.p2m.core.module.Module
import com.p2m.core.module.ModuleInfo

internal interface ModuleInfoFinder {
    fun findModuleInfo(moduleName: String): ModuleInfo?
}

internal class GlobalModuleInfoFinder(vararg moduleInfoFinder: ModuleInfoFinder): ModuleInfoFinder {
    private val moduleInfoFinders: Array<out ModuleInfoFinder> = moduleInfoFinder
    override fun findModuleInfo(moduleName: String): ModuleInfo? {
        for (moduleInfoFinder in moduleInfoFinders) {
            val moduleInfo = moduleInfoFinder.findModuleInfo(moduleName)
            if (moduleInfo != null) return moduleInfo
        }
        logW("not found module info, that name is $moduleName")
        return null
    }
}

internal class ManifestModuleInfoFinder(context: Context): ModuleInfoFinder {

    private val table = HashMap<String, ModuleInfo>()
    private val metaData =
        context
            .packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData

    @Suppress("UNCHECKED_CAST")
    override fun findModuleInfo(moduleName: String): ModuleInfo? {
        if (table.containsKey(moduleName)) {
            return table[moduleName]
        }
        val value = metaData.getString("p2m:module=${moduleName}")
            ?: throw P2MException("Not found module impl class, that name is $moduleName")

        // implModuleClass
        // publicModuleClass
        val attributes = value.trim().split(",")

        var publicModuleClass: String? = null
        var implModuleClass: String? = null
        attributes.forEach {
            val attribute = it.trim().split("=")
            val key = attribute[0].trim()
            val clazz : String = attribute[1].trim()
            logI("found $moduleName [key:$key, clazz:$clazz] in androidManifest.xml.")
            when (key) {
                "implModuleClass" -> implModuleClass = clazz
                "publicModuleClass" -> publicModuleClass = clazz
            }
        }

        if (publicModuleClass != null && implModuleClass != null) {
            return ModuleInfo(
                name = moduleName,
                publicClass = Class.forName(publicModuleClass!!) as Class<out Module<*>>,
                implClass = Class.forName(implModuleClass!!) as Class<out Module<*>>
            ).also { table[moduleName] = it }
        }
        return null
    }
}

internal class ExternalModuleInfoFinder(externalModule: Array<out ModuleInfo>) : ModuleInfoFinder {

    private val table = HashMap<String, ModuleInfo>()

    init {
        for (moduleInfo in externalModule) {
            table[moduleInfo.name] = moduleInfo
        }
    }

    override fun findModuleInfo(moduleName: String): ModuleInfo? {
        return table[moduleName]
    }
}