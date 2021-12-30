package com.p2m.core.internal.module

import com.p2m.core.exception.P2MException
import com.p2m.core.internal.log.logI
import com.p2m.core.module.Module

internal data class ModuleInfo(
    val name: String,
    val publicClass: Class<out Module<*>>,  // PACKAGE_NAME.api.MODULE_NAME
    val implClass: Class<out Module<*>>,    // PACKAGE_NAME.impl._MODULE_NAME
) {
    companion object{
        private const val MODULE_PUBLIC_PACKAGE_SUFFIX = ".p2m.api"
        private const val MODULE_IMPL_PACKAGE_SUFFIX = ".p2m.impl"

        @Suppress("UNCHECKED_CAST")
        fun fromExternal(classLoader: ClassLoader, publicClassName: String): ModuleInfo {
            try {
                val publicClass = Class.forName(publicClassName, true, classLoader) as Class<out Module<*>>
                val simpleNameDelimiterIndex = publicClassName.lastIndexOf(".")
                val publicPackageName = publicClassName.removeRange(
                    startIndex = simpleNameDelimiterIndex,
                    endIndex = publicClassName.length
                )
                check(publicPackageName.endsWith(MODULE_PUBLIC_PACKAGE_SUFFIX)) { }
                val moduleName = publicClassName.substring(simpleNameDelimiterIndex + 1)
                val packageName = publicPackageName.removeSuffix(MODULE_PUBLIC_PACKAGE_SUFFIX)
                val implClassName = "${packageName}${MODULE_IMPL_PACKAGE_SUFFIX}._${moduleName}"
                val implClass = Class.forName(implClassName, true, classLoader) as Class<out Module<*>>

                return ModuleInfo(moduleName, publicClass, implClass).also {
                    logI("find external module: $it")
                }
            }catch (t: Throwable) {
                throw P2MException("$publicClassName must conform to the naming convention of public module class, public module class name ex:PACKAGE_NAME${MODULE_PUBLIC_PACKAGE_SUFFIX}.MODULE_NAME, impl module class name ex:PACKAGE_NAME${MODULE_IMPL_PACKAGE_SUFFIX}._MODULE_NAME", t)
            }
        }
    }
}