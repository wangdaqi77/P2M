package com.p2m.compiler.processing

import com.p2m.compiler.utils.Logger
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class BaseProcessor : AbstractProcessor() {
    companion object{
        const val OPTION_MODULE_NAME = "moduleName"
        const val OPTION_APPLICATION_ID = "applicationId"
        const val OPTION_DEPENDENCIES = "dependencies"

        private const val PACKAGE_NAME_CORE = "com.p2m.core"
        const val PACKAGE_NAME_CORE_MODULE = "${PACKAGE_NAME_CORE}.module"

        const val CLASS_MODULE = "Module"
        const val CLASS_MODULE_INIT = "ModuleInit"
        const val CLASS_MODULE_INIT_EMPTY = "EmptyModuleInit"
        const val CLASS_MODULE_API = "ModuleApi"
        const val CLASS_API_LAUNCHER = "ModuleLauncher"
        const val CLASS_API_SERVICE = "ModuleService"
        const val CLASS_API_EVENT = "ModuleEvent"
        const val CLASS_API_LAUNCHER_EMPTY = "EmptyModuleLauncher"
        const val CLASS_API_SERVICE_EMPTY = "EmptyModuleService"
        const val CLASS_API_EVENT_EMPTY = "EmptyModuleEvent"

        const val PACKAGE_NAME_EVENT = "${PACKAGE_NAME_CORE}.event"
        const val CLASS_LIVE_EVENT = "LiveEvent"
        const val CLASS_BACKGROUND_EVENT = "BackgroundLiveEvent"
        const val CLASS_MUTABLE_LIVE_EVENT = "MutableLiveEvent"
        const val CLASS_MUTABLE_BACKGROUND_EVENT = "MutableBackgroundLiveEvent"
        const val CLASS_EVENT_DELEGATE = "delegate"
        const val CLASS_EVENT_MUTABLE_DELEGATE = "delegateMutable"
        const val CLASS_EVENT_INTERNAL_MUTABLE_DELEGATE = "toMutable"

        const val PACKAGE_NAME_LAUNCHER = "${PACKAGE_NAME_CORE}.launcher"
        const val CLASS_ActivityResultContractCompat = "ActivityResultContractCompat"
        const val CLASS_DefaultActivityResultContractP2MCompat = "DefaultActivityResultContractCompat"
        const val CLASS_ActivityLauncher = "ActivityLauncher"
        const val CLASS_FragmentLauncher = "FragmentLauncher"
        const val CLASS_ServiceLauncher = "ServiceLauncher"
        const val CLASS_LAUNCHER_DELEGATE = "delegate"
        const val CLASS_ILaunchActivityInterceptor = "ILaunchActivityInterceptor"
    }

    lateinit var options: Map<String, String>
    lateinit var elementUtils: Elements
    lateinit var typeUtils: Types
    lateinit var mFiler: Filer
    lateinit var mLogger: Logger
    lateinit var optionModuleName: String
    lateinit var optionApplicationId: String
    var optionDependencies: String? = null

    lateinit var packageNameApi: String
    lateinit var packageNameImpl: String
    val dependencies = hashSetOf<String>() // 依赖的模块

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        mLogger = Logger(processingEnv.messager)
        elementUtils = processingEnv.elementUtils
        typeUtils = processingEnv.typeUtils
        mFiler = processingEnv.filer
        options = processingEnv.options

        getOptionData(options)
        packageNameApi = "${optionApplicationId}.p2m.api"
        packageNameImpl = "${optionApplicationId}.p2m.impl"
        optionDependencies?.split(",")?.forEach {
            dependencies.add(it.trim())
        }
    }

    private fun getOptionData(options: Map<String, String>) {
        val optionModuleName = options[OPTION_MODULE_NAME]
        val optionApplicationId = options[OPTION_APPLICATION_ID]
        this.optionDependencies = options[OPTION_DEPENDENCIES]
        if (optionModuleName.isNullOrEmpty()) {
            mLogger.error(
                """
                    请在build.gradle添加以下内容：
                    kapt {
                            arguments {
                                arg("$OPTION_MODULE_NAME", "模块名称")
                            }
                    }
                """.trimIndent()
            )
        }else{
            this.optionModuleName = optionModuleName
        }
        if (optionApplicationId.isNullOrEmpty()) {
            mLogger.error(
                """
                    请在build.gradle添加以下内容：
                    kapt {
                            arguments {
                                arg("$OPTION_APPLICATION_ID", "你的applicationId")
                            }
                    }
                """.trimIndent()
            )
        }else{
            this.optionApplicationId = optionApplicationId
        }
    }
}
