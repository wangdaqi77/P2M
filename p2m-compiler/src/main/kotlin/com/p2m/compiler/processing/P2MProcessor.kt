package com.p2m.compiler.processing

import com.google.auto.service.AutoService
import com.p2m.annotation.module.ModuleInitializer
import com.p2m.annotation.module.api.*
import com.p2m.compiler.processing.BaseProcessor.Companion.OPTION_MODULE_NAME
import com.p2m.compiler.*
import com.p2m.compiler.bean.*
import com.p2m.compiler.processing.BaseProcessor.Companion.OPTION_APPLICATION_ID
import com.p2m.compiler.processing.BaseProcessor.Companion.OPTION_DEPENDENCIES
import com.p2m.compiler.utils.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.sun.tools.javac.code.Symbol
import java.io.File
import java.io.Writer
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.StandardLocation


@Suppress("SameParameterValue", "unused")
@KotlinPoetMetadataPreview
@AutoService(Processor::class)
@SupportedAnnotationTypes(
    "com.p2m.annotation.module.api.ApiLauncher",
    "com.p2m.annotation.module.api.ApiService",
    "com.p2m.annotation.module.api.ApiEvent",
    "com.p2m.annotation.module.api.ApiUse",
    "com.p2m.annotation.module.api.LaunchActivityInterceptor",
    "com.p2m.annotation.module.ModuleInitializer"
)
@SupportedOptions(
    OPTION_DEPENDENCIES,
    OPTION_MODULE_NAME,
    OPTION_APPLICATION_ID,
    "org.gradle.annotation.processing.aggregating"
)
class P2MProcessor : BaseProcessor() {

    companion object {
        private var TAG = "P2MProcessor"
    }

    private var genModuleInitSource = false
    private var exportApiClassPath = mutableListOf<ClassName>()
    private var exportApiSourcePath = mutableListOf<ClassName>()
    
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()){
            // collect classes of the module api scope, final they be compile
            // into jar provide to external module.
            collectModuleApiClassesToPropertiesFile()
            return true
        }

        kotlin.runCatching {

            // collect interceptor
            val interceptorsResultMap = collectInterceptors(roundEnv).also {
                for ((_, genResult) in it) {
                    exportApiClassPath.add(genResult.apiClassName)
                }
            }

            // gen module api
            val moduleApiResult = genModuleApi(roundEnv, interceptorsResultMap).also { genResult ->
                exportApiClassPath.add(genResult.apiClassName)
            }

            // gen module init
            val moduleInitResult = genModuleInit(roundEnv).also { genResult ->
                exportApiClassPath.add(genResult.apiClassName)
            }

            // gen module
            genModule(moduleApiResult, moduleInitResult, interceptorsResultMap.values).also { genResult ->
                exportApiClassPath.add(genResult.apiClassName)
            }

            // collect and provide annotated ApiUse classes for external module
            collectClassesForAnnotatedApiUse(roundEnv)

        }.apply {
            if (isFailure) {
                val throwable = exceptionOrNull()
                if (throwable != null) {
                    mLogger.error(throwable)
                } else {
                    mLogger.error("${toString()}\r\n")
                }
            }
        }

        return true
    }

    private fun collectClassesForAnnotatedApiUse(roundEnv: RoundEnvironment) {
        // find classes for annotated ApiUse
        val apiUseElements = roundEnv.getElementsAnnotatedWith(ApiUse::class.java)
        apiUseElements?.forEach { element ->
            val typeElement = element as TypeElement
            exportApiClassPath.add(typeElement.className())
        }

    }

    private fun genModuleApiProperties() = mutableMapOf<String, String>().apply {
        this["genModuleInitSource"] = "$genModuleInitSource"
        this["exportApiClassPath"] = exportApiClassPath.joinToString(",") { className ->
            // com.android.os.Test.InnerClass -> com/android/os/Test
            val prefix = className.packageName.replace(".", File.separator)
            val suffix = className.canonicalName.removePrefix(className.packageName + ".").substringBefore(".")
            prefix + File.separator + suffix
        }

        this["exportApiSourcePath"] = exportApiSourcePath.joinToString(",") { className ->
            // com.android.os.Test.InnerClass -> com/android/os/Test
            val prefix = className.packageName.replace(".", File.separator)
            val suffix = className.canonicalName.removePrefix(className.packageName + ".").substringBefore(".")
            prefix + File.separator + suffix
        }
    }

    private fun genModule(
        moduleApiResult: GenResult,
        moduleInitResult: GenResult,
        interceptorsResult: Collection<GenResult>
    ): GenResult {
        val ModuleClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_MODULE")
        val apiPackageName = packageNameApi
        val apiFileName = optionModuleName
        val implPackageName = packageNameImpl
        val implFileName = "_${apiFileName}"

        val apiFileSpecBuilder = FileSpec
            .builder(apiPackageName, apiFileName)
            .addFileComment()
        exportApiSourcePath.add(ClassName(apiPackageName, apiFileName))

        val implFileSpecBuilder = FileSpec
            .builder(implPackageName, implFileName)
            .addFileComment()

        return genModuleClassForKotlin(
            moduleApiResult,
            moduleInitResult,
            interceptorsResult,
            ModuleClassName,
            apiPackageName,
            apiFileName,
            implPackageName,
            implFileName,
            apiFileSpecBuilder,
            implFileSpecBuilder
        ).apply {
            apiFileSpecBuilder.build().writeTo(mFiler)
            implFileSpecBuilder.build().writeTo(mFiler)
        }
    }

    private fun genModuleClassForKotlin(
        moduleApiResult: GenResult,
        moduleInitResult: GenResult,
        interceptorsResult: Collection<GenResult>,
        moduleClassName: ClassName,
        apiPackageName: String,
        apiName: String,
        implPackageName: String,
        implName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        implFileSpecBuilder: FileSpec.Builder
    ): GenResult {
        val ModuleInitClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_MODULE_INIT")
        val apiClassName = ClassName(apiPackageName, apiName)
        TypeSpec.classBuilder(apiClassName)
            .addKdoc(
                CodeBlock.of(
                    """
                        A public module class of $apiName.
                        
                        Use `P2M.apiOf($apiName)` to get instance of its api.
                        
                        @see %T - api.
                    """.trimIndent(),
                    moduleApiResult.apiClassName
                )
            )
            .addModifiers(KModifier.ABSTRACT)
            .superclass(
                moduleClassName.parameterizedBy(
                    moduleApiResult.apiClassName
                )
            )
            .build()
            .run(apiFileSpecBuilder::addType)


        val implClassName = ClassName(implPackageName, implName)
        TypeSpec.classBuilder(implClassName)
            .superclass(apiClassName)
            .addProperty(
                PropertySpec.builder("api", moduleApiResult.apiClassName)
                    .addModifiers(KModifier.OVERRIDE)
                    .delegate("lazy { ${moduleApiResult.getImplInstanceStatement()} }")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("init", ModuleInitClassName)
                    .addModifiers(KModifier.OVERRIDE)
                    .addModifiers(KModifier.PROTECTED)
                    .delegate("lazy { ${moduleInitResult.getImplInstanceStatement()} }")
                    .build()
            )
            .apply {
                dependencies.forEach{
                    addInitializerBlock(
                        CodeBlock.of(
                            "dependOn(\"%L\")",
                            it
                        )
                    )
                }

                interceptorsResult.forEach {
                    addInitializerBlock(
                        CodeBlock.of(
                            "collectInterceptorForLaunchActivity(%T::class, %T())",
                            it.apiClassName,
                            it.implClassName
                        )
                    )
                }
            }
            .build()
            .run(implFileSpecBuilder::addType)

        return GenResult(apiClassName, implClassName)
    }

    private fun genModuleInit(roundEnv: RoundEnvironment): GenResult {
        val ModuleInitClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_MODULE_INIT")
        val apiPackageName = packageNameApi
        val apiFileName = "${optionModuleName}ModuleInit"
        val implPackageName = packageNameImpl
        val implFileName = "_${apiFileName}"

        val moduleInitElement = roundEnv.getSingleTypeElementAnnotatedWith(
            mLogger,
            optionModuleName,
            ModuleInitializer::class.java
        ) as? TypeElement

        check(moduleInitElement != null) {
            """
                Must add source code in Module[${optionModuleName}]:
                
                @ModuleInitializer
                class ${optionModuleName}ModuleInit : ModuleInit{

                    override fun onEvaluate(context: Context, taskRegister: TaskRegister) {
                        // Evaluate stage of itself.
                        // Here, You can use [taskRegister] to register tasks for help initialize module fast,
                        // and they will be executed order.
                    }

                    override fun onExecuted(context: Context, taskOutputProvider: TaskOutputProvider) {
                        // Executed stage of itself, indicates will completed initialized of the module.
                        // Called when its all tasks be completed and all dependencies completed initialized.
                        // Here, You can use [taskOutputProvider] to get some output of itself tasks.
                        // More important to ensure its `Api` area safely.
                    }
                }
                
                open https://github.com/wangdaqi77/P2M to see more.
            """.trimIndent()
        }

        moduleInitElement.checkKotlinClass()
        val apiFileSpecBuilder = FileSpec
            .builder(apiPackageName, apiFileName)
            .addFileComment()
        exportApiSourcePath.add(ClassName(apiPackageName, apiFileName))

        val implFileSpecBuilder = FileSpec
            .builder(implPackageName, implFileName)
            .addFileComment()

        return genModuleInitClassForKotlin(
            moduleInitElement,
            apiPackageName,
            apiFileName,
            implPackageName,
            implFileName,
            ModuleInitClassName,
            apiFileSpecBuilder,
            implFileSpecBuilder
        ).also {
            apiFileSpecBuilder.build().writeTo(mFiler)
            implFileSpecBuilder.build().writeTo(mFiler)
            genModuleInitSource = true
        }
    }

    private fun collectInterceptors(roundEnv: RoundEnvironment): Map<TypeElement, GenResult> {
        val ILaunchActivityInterceptorName = ClassName.bestGuess("$PACKAGE_NAME_LAUNCHER.$CLASS_ILaunchActivityInterceptor")
        val apiPackageName = packageNameApi
        val apiFileName = "${optionModuleName}Interceptors"
        val implPackageName = packageNameImpl
        val implFileName = "_${apiFileName}"

        val elements = roundEnv.getElementsAnnotatedWith(LaunchActivityInterceptor::class.java)
        if (elements.isEmpty()) return emptyMap()

        val apiFileSpecBuilder = FileSpec
            .builder(apiPackageName, apiFileName)
            .addFileComment()
        exportApiSourcePath.add(ClassName(apiPackageName, apiFileName))

        val implFileSpecBuilder = FileSpec
            .builder(implPackageName, implFileName)
            .addFileComment()

        return elements.map{ element ->
            element as TypeElement
            element.checkKotlinClass()

            val annotation = element.getAnnotation(LaunchActivityInterceptor::class.java)
            LaunchActivityInterceptor.checkName(annotation, element.qualifiedName.toString())
            val apiName = "${optionModuleName}LaunchActivityInterceptorFor${annotation.interceptorName}"
            element to genInterceptorClassForKotlin(
                element,
                apiPackageName,
                apiName,
                implPackageName,
                "_${apiName}",
                ILaunchActivityInterceptorName,
                apiFileSpecBuilder,
                implFileSpecBuilder
            )
        }
            .let { mapOf(*it.toTypedArray()) }
            .also {
            apiFileSpecBuilder.build().writeTo(mFiler)
            implFileSpecBuilder.build().writeTo(mFiler)
        }
    }

    private fun genModuleApi(
        roundEnv: RoundEnvironment,
        interceptorsResultMap: Map<TypeElement, GenResult>
    ): GenResult {
        // of p2m-core
        val ModuleApiClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_MODULE_API")
        val ModuleLauncherClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_API_LAUNCHER")
        val ModuleServiceClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_API_SERVICE")
        val ModuleEventClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_API_EVENT")
        val EmptyLauncherClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_API_LAUNCHER_EMPTY")
        val EmptyServiceClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_API_SERVICE_EMPTY")
        val EmptyEventClassName = ClassName.bestGuess("$PACKAGE_NAME_CORE_MODULE.$CLASS_API_EVENT_EMPTY")

        // for generated
        val apiPackageName = packageNameApi
        val implPackageName = packageNameImpl
        val apiFileName = "${optionModuleName}ModuleApi"
        val implFileName = "_${optionModuleName}ModuleApi"

        // of api kt file, need export
        val apiFileSpecBuilder = FileSpec
            .builder(apiPackageName, apiFileName)
            .addFileComment()
        exportApiSourcePath.add(ClassName(apiPackageName, apiFileName))

        // of impl kt file
        val implFileSpecBuilder = FileSpec
            .builder(implPackageName, implFileName)
            .addFileComment()

        // gen launcher
        val genLauncherResult: GenResult = genLauncherClassForKotlin(
            roundEnv,
            interceptorsResultMap,
            EmptyLauncherClassName,
            ModuleLauncherClassName,
            apiPackageName,
            implPackageName,
            apiFileSpecBuilder,
            implFileSpecBuilder
        )

        // gen service
        val genServiceResult = genServiceClassForKotlin(
            roundEnv,
            EmptyServiceClassName,
            ModuleServiceClassName,
            apiPackageName,
            implPackageName,
            apiFileSpecBuilder,
            implFileSpecBuilder
        )

        // gen event
        val genEventResult = genEventClassForKotlin(
            roundEnv,
            EmptyEventClassName,
            ModuleEventClassName,
            apiPackageName,
            implPackageName,
            apiFileSpecBuilder,
            implFileSpecBuilder
        )

        // gen module api
        return genModuleApiClassForKotlin(
            ModuleApiClassName,
            apiPackageName,
            implPackageName,
            apiFileName,
            genLauncherResult,
            genServiceResult,
            genEventResult,
            apiFileSpecBuilder,
            implFileSpecBuilder
        ).also {
            // write for api
            apiFileSpecBuilder.build().writeTo(mFiler)
            implFileSpecBuilder.build().writeTo(mFiler)

            // copy source to provide for external module
            // mLogger.info("apiSrcDir:$apiSrcDir\r\n")
            // if (!apiSrcDir.exists()) apiSrcDir.mkdirs()
            // apiFileSpecBuilder.build().writeTo(apiSrcDir)
        }
    }

    private fun collectModuleApiClassesToPropertiesFile() {
        val propertiesFile = mFiler.createResource(StandardLocation.SOURCE_OUTPUT, "",
            FILE_NAME_PROPERTIES
        )
        propertiesFile.openWriter().use(::writeModuleApiClassesProperties)
//        val apiSrcDirPath = apiSrcDir.toPath()
//        Files.createDirectories(apiSrcDirPath)
//        val outputPath = apiSrcDirPath.resolve(FILE_NAME_PROPERTIES)
//        OutputStreamWriter(Files.newOutputStream(outputPath), StandardCharsets.UTF_8).use(::writeModuleApiProperties)
    }

    private fun writeModuleApiClassesProperties(writer: Writer) {
        genModuleApiProperties().forEach { (attr, value) ->
            writer.write("${attr}=${value}\n")
            // mLogger.info("$optionModuleName -> $FILE_NAME_PROPERTIES ${attr}=${value}\n")
        }
    }

    private fun genModuleInitClassForKotlin(
        moduleInitElement: TypeElement,
        apiPackageName: String,
        apiName: String,
        implPackageName: String,
        implName: String,
        ModuleInitClassName: ClassName,
        apiFileSpecBuilder: FileSpec.Builder,
        implFileSpecBuilder: FileSpec.Builder
    ): GenResult {

        val moduleInitClassNameOrigin = ClassName(moduleInitElement.packageName(), moduleInitElement.simpleName.toString())

        check(moduleInitElement.interfaces.size != 0) { "${moduleInitElement.qualifiedName} must implement ${ModuleInitClassName.canonicalName}" }
        check(moduleInitElement.interfaces.size == 1) { "${moduleInitElement.qualifiedName} implement ${ModuleInitClassName.canonicalName} only is allowed." }
        check(moduleInitElement.interfaces[0].toString() == ModuleInitClassName.canonicalName) { "${moduleInitElement.qualifiedName} must implement ${ModuleInitClassName.canonicalName}" }

        //接口
        val moduleInitApiClassName = ClassName(apiPackageName, apiName)
        TypeSpec.interfaceBuilder(moduleInitApiClassName)
            .addSuperinterface(ModuleInitClassName)
            .build()
            .run(apiFileSpecBuilder::addType)

        // 服务代理类，代理被注解的类
        val moduleInitImplClassName = ClassName(implPackageName, implName)
        TypeSpec.classBuilder(moduleInitImplClassName)
            .addSuperinterface(moduleInitApiClassName)
            .addSuperinterface(ModuleInitClassName, delegate = CodeBlock.of("%T()", moduleInitClassNameOrigin))
            .build()
            .run(implFileSpecBuilder::addType)

        return GenResult(moduleInitApiClassName, moduleInitImplClassName)
    }

    private fun genInterceptorClassForKotlin(
        element: TypeElement,
        apiPackageName: String,
        apiName: String,
        implPackageName: String,
        implName: String,
        ILaunchActivityInterceptorName: ClassName,
        apiFileSpecBuilder: FileSpec.Builder,
        implFileSpecBuilder: FileSpec.Builder
    ): GenResult {

        val interceptorClassNameOrigin = ClassName(element.packageName(), element.simpleName.toString())

        check(element.interfaces.size != 0) { "${element.qualifiedName} must implement ${ILaunchActivityInterceptorName.canonicalName}" }
        check(element.interfaces.size == 1) { "${element.qualifiedName} implement ${ILaunchActivityInterceptorName.canonicalName} only is allowed." }
        check(element.interfaces[0].toString() == ILaunchActivityInterceptorName.canonicalName) { "${element.qualifiedName} must implement ${ILaunchActivityInterceptorName.canonicalName}" }
        val kdoc = elementUtils.getKDoc(element)
        //接口
        val interceptorApiClassName = ClassName(apiPackageName, apiName)
        TypeSpec.interfaceBuilder(interceptorApiClassName)
            .addSuperinterface(ILaunchActivityInterceptorName)
            .apply {
                kdoc?.run(::addKdoc)
                addKdoc("@see %T - origin.", interceptorClassNameOrigin)
            }
            .build()
            .run(apiFileSpecBuilder::addType)

        // 服务代理类，代理被注解的类
        val interceptorImplClassName = ClassName(implPackageName, implName)
        TypeSpec.classBuilder(interceptorImplClassName)
            .addSuperinterface(interceptorApiClassName)
            .addSuperinterface(ILaunchActivityInterceptorName, delegate = CodeBlock.of("%T()", interceptorClassNameOrigin))
            .build()
            .run(implFileSpecBuilder::addType)

        return GenResult(interceptorApiClassName, interceptorImplClassName)
    }

    private fun genModuleApiClassForKotlin(
        ModuleApiClassName: ClassName,
        apiPackageName: String,
        implPackageName: String,
        apiFileName: String,
        genModuleLauncherResult: GenResult,
        genModuleServiceResult: GenResult,
        genModuleEventResult: GenResult,
        moduleInterfaceFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder
    ): GenResult {
        val apiClassName = ClassName(apiPackageName, apiFileName)
        val implClassName = ClassName(implPackageName, "_${apiFileName}")

        val superModuleApiParameterizedTypeName = ModuleApiClassName.parameterizedBy(
            genModuleLauncherResult.apiClassName,
            genModuleServiceResult.apiClassName,
            genModuleEventResult.apiClassName
        )

        val launcherVarName = "launcher"
        val serviceVarName = "service"
        val eventVarName = "event"

        val apiTypeSpecBuilder = TypeSpec
            .interfaceBuilder(apiClassName)
            .addSuperinterface(superModuleApiParameterizedTypeName)
            .addKdoc("A api class of $optionModuleName module.\n")
            .addKdoc("\n")
            .addKdoc("Use `P2M.apiOf(${optionModuleName})` to get the instance.\n")
            .addKdoc("\n")
            .addKdoc("@see %T - $launcherVarName, use `P2M.apiOf($optionModuleName).$launcherVarName` to get the instance.\n", genModuleLauncherResult.apiClassName)
            .addKdoc("@see %T - $serviceVarName, use `P2M.apiOf($optionModuleName).$serviceVarName` to get the instance.\n", genModuleServiceResult.apiClassName)
            .addKdoc("@see %T - $eventVarName, use `P2M.apiOf($optionModuleName).$eventVarName` to get the instance.\n", genModuleEventResult.apiClassName)

        val apiTypeSpec = apiTypeSpecBuilder.build()

        val launcherProperty = PropertySpec.builder(
            launcherVarName,
            genModuleLauncherResult.apiClassName,
            KModifier.OVERRIDE
        ).mutable(false).delegate("lazy() { ${genModuleLauncherResult.getImplInstanceStatement()} }").build()

        val serviceProperty = PropertySpec.builder(
            serviceVarName,
            genModuleServiceResult.apiClassName,
            KModifier.OVERRIDE
        ).mutable(false).delegate("lazy() { ${genModuleServiceResult.getImplInstanceStatement()} }").build()

        val eventProperty = PropertySpec.builder(
            eventVarName,
            genModuleEventResult.apiClassName,
            KModifier.OVERRIDE
        ).mutable(false).delegate("lazy() { ${genModuleEventResult.getImplInstanceStatement()} }").build()

        // 模块实现类
        val implTypeSpecBuilder = TypeSpec
            .classBuilder(implClassName)
            .addSuperinterface(apiClassName)
            .addProperty(launcherProperty)
            .addProperty(serviceProperty)
            .addProperty(eventProperty)

        val implTypeSpec = implTypeSpecBuilder.build()

        moduleInterfaceFileSpecBuilder.addType(apiTypeSpec)
        apiImplFileSpecBuilder.addType(implTypeSpec)

        return GenResult(apiClassName, implClassName)
    }

    private fun genLauncherClassForKotlin(
        roundEnv: RoundEnvironment,
        interceptorsResultMap: Map<TypeElement, GenResult>,
        EmptyLauncherClassName: ClassName,
        ModuleLauncherClassName: ClassName,
        apiPackageName: String,
        implPackageName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder
    ): GenResult {
        val apiName = "${optionModuleName}ModuleLauncher"
        val elements = roundEnv.getElementsAnnotatedWith(ApiLauncher::class.java)
        return if (elements.isEmpty()) {
            GenResult(
                EmptyLauncherClassName,
                EmptyLauncherClassName,
                true
            )
        } else {
            genLauncherClassForKotlin(
                interceptorsResultMap,
                elements,
                ModuleLauncherClassName,
                apiName,
                apiPackageName,
                implPackageName,
                apiFileSpecBuilder,
                apiImplFileSpecBuilder
            ).also {
                exportApiClassPath.add(it.apiClassName)
            }
        }

    }

    private fun genLauncherClassForKotlin(
        interceptorsResultMap: Map<TypeElement, GenResult>,
        elements: Set<Element>,
        ModuleLauncherClassName: ClassName,
        apiName: String,
        apiPackageName: String,
        implPackageName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder
    ): GenResult {
        val activityTm = elementUtils.getTypeElement(CLASS_ACTIVITY).asType()
        val serviceTm = elementUtils.getTypeElement(CLASS_SERVICE).asType()
        val fragmentTm = elementUtils.getTypeElement(CLASS_FRAGMENT).asType()
        val fragmentTmV4 = elementUtils.getTypeElement(CLASS_FRAGMENT_V4).asType()
        val fragmentTmAndroidX = elementUtils.getTypeElement(CLASS_FRAGMENT_ANDROID_X).asType()
        val defaultActivityResultContractP2MCompatTm = elementUtils.getTypeElement("${PACKAGE_NAME_LAUNCHER}.${CLASS_DefaultActivityResultContractP2MCompat}").asType()
        val Intent = ClassName.bestGuess(CLASS_INTENT)

        val ActivityLauncher = ClassName(PACKAGE_NAME_LAUNCHER, CLASS_ActivityLauncher)
        val ServiceLauncher = ClassName(PACKAGE_NAME_LAUNCHER, CLASS_ServiceLauncher)
        val FragmentLauncher = ClassName(PACKAGE_NAME_LAUNCHER, CLASS_FragmentLauncher)
        val ActivityLauncherDelegate = ClassName.bestGuess("$ActivityLauncher.$CLASS_LAUNCHER_DELEGATE")
        val ServiceLauncherDelegate = ClassName.bestGuess("$ServiceLauncher.$CLASS_LAUNCHER_DELEGATE")
        val FragmentLauncherDelegate = ClassName.bestGuess("$FragmentLauncher.$CLASS_LAUNCHER_DELEGATE")
        val ActivityResultContractP2MCompat = ClassName(PACKAGE_NAME_LAUNCHER, CLASS_ActivityResultContractCompat)


        val activityResultContractTypeArgumentsCache = mutableMapOf<String, List<TypeName>>()
        val activityResultContractTypeCache = mutableMapOf<String, String>()
        // 模块内部拦截器映射 key: originInterceptor  value: genResult
        val moduleInternalActivityInterceptorsCache = mutableMapOf<ClassName, GenResult>()

        // 接口
        val apiPropertySpecs = elements.map { element ->
            element as TypeElement
            element.checkKotlinClass()

            val tm = element.asType()
            val className = element.className()
            findAnnotationValue(
                element = element,
                annotationClass = ApiLauncher::class.qualifiedName!!,
                valueName = "launchActivityInterceptor",
                expectedType = com.sun.tools.javac.util.List::class.java,
            )?.takeIf { it.nonEmpty() }?.forEach {
                if (it.toString() == "<error>") {
                    throw IllegalArgumentException("Please use the interceptor declared inside the module, see @ApiLauncher(launchActivityInterceptor = ...) in $className")
                }

                val interceptorClassName = ClassName.bestGuess(it.toString().removeSuffix(".class"))
                interceptorsResultMap.keys.find { it.className() == interceptorClassName }
                    ?.let { internalActivityInterceptorElement ->
                        moduleInternalActivityInterceptorsCache[interceptorClassName] =
                            interceptorsResultMap[internalActivityInterceptorElement]!!
                    }
            }

            val launcherAnnotation = element.getAnnotation(ApiLauncher::class.java)
            val launcherName = launcherAnnotation.launcherName
            // activityResultContract
            try {
                val activityResultContract = launcherAnnotation.activityResultContract
                val activityResultContractSpec = activityResultContract.toImmutableKmClass().toTypeSpec(null)
                activityResultContractTypeCache[launcherName] = activityResultContract.qualifiedName!!
                activityResultContractTypeArgumentsCache[launcherName] = (activityResultContractSpec.superclass as ParameterizedTypeName).typeArguments
            }catch (e: Throwable) {
                // Attempt to access Class object for TypeMirror com.p2m.core.launcher.DefaultActivityResultContractCompat
                val allowErrorPrefix = "Attempt to access Class object for TypeMirror "

                val message = e.message ?: throw e
                if (!message.startsWith(allowErrorPrefix)) throw e
//                        mLogger.info("allow error: $message\r\n")
                val activityResultContractName = message.removePrefix(allowErrorPrefix)
                if (activityResultContractName == defaultActivityResultContractP2MCompatTm.toString()) {
                    activityResultContractTypeCache[launcherName] = activityResultContractName
                    activityResultContractTypeArgumentsCache[launcherName] = listOf(Intent, Intent)
                } else {
                    val customActivityResultContractElement = elementUtils.getTypeElement(activityResultContractName)
                    val customTypeSpec = customActivityResultContractElement.toTypeSpec()
                    check(customTypeSpec.superclass.toString().startsWith(ActivityResultContractP2MCompat.canonicalName)) {
                        "The super class of ${customActivityResultContractElement.qualifiedName} must is ${ActivityResultContractP2MCompat.canonicalName}, current: ${customTypeSpec.superclass}."
                    }
                    val parameterizedTypeName = customActivityResultContractElement.toTypeSpec().superclass as ParameterizedTypeName
                    activityResultContractTypeCache[launcherName] = activityResultContractName
                    activityResultContractTypeArgumentsCache[launcherName] = parameterizedTypeName.typeArguments
                }
            }

            ApiLauncher.checkName(launcherAnnotation, className.canonicalName)
            val builder =  when {
                typeUtils.isSubtype(element.asType(), activityTm) -> { // Activity
                    /*
                     * val activityOf$launcherName: ActivityLauncher<I, O>
                     */
                    PropertySpec.builder(
                        name = "activityOf$launcherName",
                        type = ActivityLauncher.parameterizedBy(activityResultContractTypeArgumentsCache[launcherName]!!)
                    )
                        .mutable(false)
                        .apply {
                            elementUtils.getKDoc(element)?.apply { addKdoc(this) }
                            addKdoc("@see %T - origin.\n", className)
                            addKdoc("@see %L - activity result contract.", activityResultContractTypeCache[launcherName]!!)
                        }
                }
                typeUtils.isSubtype(tm, fragmentTm)
                        || typeUtils.isSubtype(tm, fragmentTmV4)
                        || typeUtils.isSubtype(tm, fragmentTmAndroidX) -> {
                    val fragmentClassName = ClassName.bestGuess(
                        when {
                            typeUtils.isSubtype(tm, fragmentTmV4) -> CLASS_FRAGMENT_V4
                            typeUtils.isSubtype(tm, fragmentTmAndroidX) -> CLASS_FRAGMENT_ANDROID_X
                            typeUtils.isSubtype(tm, fragmentTm) -> CLASS_FRAGMENT
                            else -> CLASS_FRAGMENT
                        }
                    )

                    /*
                    * val fragmentOf$launcherName: FragmentLauncher<Fragment>
                    */
                    PropertySpec.builder(name = "fragmentOf$launcherName", type = FragmentLauncher.parameterizedBy(fragmentClassName))
                        .mutable(false)
                        .apply {
                            elementUtils.getKDoc(element)?.apply { addKdoc(this) }
                            addKdoc("@see %T - origin.", className)
                        }
                }
                typeUtils.isSubtype(tm, serviceTm) -> { // Service
                    /*
                    * val serviceOf$launcherName: ServiceLauncher
                    */
                    PropertySpec.builder(name = "serviceOf$launcherName", type = ServiceLauncher)
                        .mutable(false)
                        .apply {
                            elementUtils.getKDoc(element)?.apply { addKdoc(this) }
                            addKdoc("@see %T - origin.", className)
                        }
                }
                else -> throw IllegalArgumentException("@ApiLauncher not support in ${className.canonicalName}.")
            }
            builder.build()
        }
        val apiClassName = ClassName(apiPackageName, apiName)
        val apiTypeSpec = TypeSpec.interfaceBuilder(apiClassName).run {
            addSuperinterface(ModuleLauncherClassName)
            addProperties(apiPropertySpecs)
            build()
        }

        // 启动器实现类
        val implClassName = ClassName(implPackageName, "_${apiName}")
        val implPropertySpecs = elements.map { element ->
            val tm = element.asType()
            val launcherAnnotation = element.getAnnotation(ApiLauncher::class.java)
            val className = element.className()
            val launcherName = launcherAnnotation.launcherName
            ApiLauncher.checkName(launcherAnnotation, className.canonicalName)
            val builder =  when {
                typeUtils.isSubtype(tm, activityTm) -> { // Activity
                    /*
                     * val activityOf$launcherName: ActivityLauncher<I, O> by lazy(ActivityLauncher.Delegate(XX::class.java, XX::class.java))
                     */
                    try {
                        val launchActivityInterceptors = launcherAnnotation.launchActivityInterceptor
                        val launchActivityInterceptorsStr = launchActivityInterceptors
                            .takeIf { it.isNotEmpty() }
                            ?.map { kClass -> kClass.asClassName() }
                            ?.map { moduleInternalActivityInterceptorsCache[it]?.apiClassName ?: it }
                            ?.joinToString { ", ${it.canonicalName}::class" }
                            ?: ""
                        PropertySpec.builder(
                            name = "activityOf$launcherName",
                            type = ActivityLauncher.parameterizedBy(
                                activityResultContractTypeArgumentsCache[launcherName]!!
                            )
                        )
                            .addModifiers(KModifier.OVERRIDE)
                            .mutable(false)
                            .delegate(
                                "%T(%T::class.java%L) { %L() }",
                                ActivityLauncherDelegate,
                                className,
                                launchActivityInterceptorsStr,
                                activityResultContractTypeCache[launcherName]!!
                            )
                    }catch (e: Throwable) {
                        // Attempt to access Class objects for TypeMirrors []
                        val allowErrorPrefix = "Attempt to access Class objects for TypeMirrors "
                        val message = e.message ?: throw e
                        if (!message.startsWith(allowErrorPrefix)) throw e
//                        mLogger.info("allow error: $message\r\n")
                        val launchActivityInterceptorsStr = message.removePrefix(allowErrorPrefix)
                            .removePrefix("[").removeSuffix("]")
                            .takeIf { it.isNotEmpty() }
                            ?.split(",")
                            ?.map { name -> ClassName.bestGuess(name.trim()) }
                            ?.map { moduleInternalActivityInterceptorsCache[it]?.apiClassName ?: it }
                            ?.joinToString { ", ${it.canonicalName}::class" }
                            ?: ""
                        PropertySpec.builder(
                            name = "activityOf$launcherName",
                            type = ActivityLauncher.parameterizedBy(
                                activityResultContractTypeArgumentsCache[launcherName]!!
                            )
                        )
                            .addModifiers(KModifier.OVERRIDE)
                            .mutable(false)
                            .delegate(
                                "%T(%T::class.java%L) { %L() }",
                                ActivityLauncherDelegate,
                                className,
                                launchActivityInterceptorsStr,
                                activityResultContractTypeCache[launcherName]!!
                            )
                    }
                }
                typeUtils.isSubtype(tm, fragmentTm)
                        || typeUtils.isSubtype(tm, fragmentTmV4)
                        || typeUtils.isSubtype(tm, fragmentTmAndroidX) -> {
                    val fragmentClassName = ClassName.bestGuess(
                        when {
                            typeUtils.isSubtype(tm, fragmentTmV4) -> CLASS_FRAGMENT_V4
                            typeUtils.isSubtype(tm, fragmentTmAndroidX) -> CLASS_FRAGMENT_ANDROID_X
                            typeUtils.isSubtype(tm, fragmentTm) -> CLASS_FRAGMENT
                            else -> CLASS_FRAGMENT
                        }
                    )

                    /*
                    * val fragmentOf$launcherName: FragmentLauncher<Fragment> by lazy(FragmentLauncher.Delegate{ XX() }})
                    */
                    PropertySpec.builder(name = "fragmentOf$launcherName", type = FragmentLauncher.parameterizedBy(fragmentClassName))
                        .addModifiers(KModifier.OVERRIDE)
                        .mutable(false)
                        .delegate("%T{ %T() }", FragmentLauncherDelegate, className)
                }
                typeUtils.isSubtype(tm, serviceTm) -> { // Service
                    /*
                    * val serviceOf$launcherName: ServiceLauncher by lazy(ServiceLauncher.Delegate(XX::class.java))
                    */
                    PropertySpec.builder(name = "serviceOf$launcherName", type = ServiceLauncher)
                        .addModifiers(KModifier.OVERRIDE)
                        .mutable(false)
                        .delegate("%T(%T::class.java)", ServiceLauncherDelegate, className)
                }
                else -> throw IllegalArgumentException("@ApiLauncher not support in ${className.canonicalName}.")
            }
            builder.run {
                build()
            }
        }

        val implTypeSpec = TypeSpec.classBuilder(name = implClassName.simpleName).run {
            addSuperinterface(apiClassName)
            addProperties(implPropertySpecs)
            build()
        }

        apiFileSpecBuilder.addType(apiTypeSpec.toBuilder().run {
            addKdoc("A launcher class of $optionModuleName module.\n")
            addKdoc("\n")
            addKdoc("Use `P2M.apiOf(${optionModuleName}).launcher` to get the instance.\n")
            build()
        })
        apiImplFileSpecBuilder.addType(implTypeSpec)
        return GenResult(apiClassName, implClassName)
    }

    private fun genServiceClassForKotlin(
        roundEnv: RoundEnvironment,
        EmptyServiceClassName: ClassName,
        ModuleServiceClassName: ClassName,
        apiPackageName: String,
        implPackageName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder
    ): GenResult {
        val serviceElement = roundEnv.getSingleTypeElementAnnotatedWith(
            mLogger,
            optionModuleName,
            ApiService::class.java
        ) as? TypeElement
        return if (serviceElement == null) {
            GenResult(
                EmptyServiceClassName,
                EmptyServiceClassName,
                true
            )
        } else {
            serviceElement.checkKotlinClass()

            genServiceClassForKotlin(
                ModuleServiceClassName,
                serviceElement,
                apiPackageName,
                implPackageName,
                apiFileSpecBuilder,
                apiImplFileSpecBuilder
            ).also {
                exportApiClassPath.add(it.apiClassName)
            }
        }
    }

    private fun genServiceClassForKotlin(
        ModuleServiceClassName: ClassName,
        serviceElement: TypeElement,
        apiPackageName: String,
        implPackageName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder
    ): GenResult {

        // service类型源
        val serviceTypeSpecOrigin = serviceElement.toTypeSpec().also {
            check(it.kind == TypeSpec.Kind.CLASS) {
                "${serviceElement.qualifiedName} must is a class."
            }
        }

        val serviceClassNameOrigin = serviceElement.className()

        // 服务接口
        val apiName = "${optionModuleName}ModuleService"

        val notSupportedModifier = mutableSetOf(
            Modifier.PRIVATE,
            Modifier.DEFAULT,
            Modifier.PROTECTED,
            Modifier.STATIC,
            Modifier.ABSTRACT
        )

        var constructorCount = 0
        val methodDocMap = mutableMapOf(*(serviceElement.enclosedElements
            .filter { element ->

                // check constructor
                if (element is Symbol.MethodSymbol && element.simpleName.toString() == "<init>") {
                    check (constructorCount++ == 0) {"Not has multi constructor, at ${serviceClassNameOrigin.canonicalName}"}
                    check (element.params.isEmpty()) {"Params of constructor must empty, at ${serviceClassNameOrigin.canonicalName}"}
                }

                // modifiers filter
                element is Symbol.MethodSymbol && element.modifiers.toMutableSet().let{
                    val size = it.size
                    it.removeAll(notSupportedModifier)

                    (size == it.size).also {include-> check(include) { "Not supported modifies of $it at ${element.qualifiedName}." } }
                }
            }
            .map { element ->
                val methodSymbol = element as Symbol.MethodSymbol
                val kDoc = elementUtils.getKDoc(methodSymbol)
                val sign = methodSymbol.simpleName.toString() + methodSymbol.params.map { it.name }.toString()
                sign to kDoc
            }.toTypedArray())
        )

        val validFunSpecs= serviceTypeSpecOrigin.funSpecs
            .filter { funSpec ->
                val sign = funSpec.name + funSpec.parameters.map { it.name }.toString()
                methodDocMap.containsKey(sign)
            }

        val apiFunSpecs = validFunSpecs.map { funSpec ->
            funSpec.toBuilder().run {
                annotations.clear()
                clearBody().addModifiers(KModifier.ABSTRACT)
                val sign = funSpec.name + funSpec.parameters.map { it.name }.toString()
                val kDoc =  methodDocMap[sign]
                kDoc?.run(::addKdoc)
                addKdoc("@see %T.%L - origin.", serviceClassNameOrigin, funSpec.name)
                build()
            }
        }

        val apiClassName = ClassName(apiPackageName, apiName)
        val apiTypeSpec = TypeSpec.interfaceBuilder(apiClassName).run {
            addSuperinterface(ModuleServiceClassName)
            funSpecs.clear()
            addFunctions(apiFunSpecs)
            build()
        }

        // 服务代理类，代理被注解的类
        val implClassName = ClassName(implPackageName, "_${apiName}")
        val serviceRealRefName = "serviceReal"
        val serviceRealProperty = PropertySpec.builder(
            serviceRealRefName,
            serviceClassNameOrigin,
            KModifier.PRIVATE
        ).mutable(false).delegate("lazy() { %T() }", serviceClassNameOrigin).build()

        val implFunSpecs = validFunSpecs.map { funSpec ->
            funSpec.toBuilder().run {
                modifiers.remove(KModifier.ABSTRACT)
                addModifiers(KModifier.OVERRIDE)
                clearBody()
                addStatement(
                    "return %L.%L(%L)",
                    serviceRealRefName,
                    funSpec.name,
                    funSpec.parameters.convertRealParamsForKotlin()
                )
                build()
            }
        }

        val implTypeSpec = apiTypeSpec.toBuilder(TypeSpec.Kind.CLASS, name = implClassName.simpleName).run {
            addSuperinterface(apiClassName)
            addProperty(serviceRealProperty)
            funSpecs.clear()
            addFunctions(implFunSpecs)
            build()
        }

        apiFileSpecBuilder.addType(apiTypeSpec.toBuilder().run {
            addKdoc("A service class of $optionModuleName module.\n")
            addKdoc("\n")
            addKdoc("Use `P2M.apiOf(${optionModuleName}).service` to get the instance.\n")
            addKdoc("\n")
            addKdoc("@see %T - origin.", serviceClassNameOrigin)
            addKdoc("\n")
            build()
        })

        apiImplFileSpecBuilder.addType(implTypeSpec)
        return GenResult(apiClassName, implClassName)
    }

    private fun genEventClassForKotlin(
        roundEnv: RoundEnvironment,
        EmptyEventClassName: ClassName,
        ModuleEventClassName: ClassName,
        apiPackageName: String,
        implPackageName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder
    ): GenResult {
        val eventElement = roundEnv.getSingleTypeElementAnnotatedWith(
            mLogger,
            optionModuleName,
            ApiEvent::class.java
        ) as? TypeElement

        eventElement?.checkKotlinClass()

        val eventFieldMap = eventElement?.run {
            val syntheticMethodForAnnotationsMap = mutableMapOf<String, String>()
            toImmutableKmClass().properties
                .filter { it.syntheticMethodForAnnotations != null }
                .forEach {
                    syntheticMethodForAnnotationsMap[it.syntheticMethodForAnnotations!!.name] = it.name
                }
            val syntheticMethodForAnnotations = syntheticMethodForAnnotationsMap.keys

            val eventFieldElements = roundEnv.getElementsAnnotatedWith(ApiEventField::class.java)
            mutableMapOf(
                *(eventFieldElements.filter { syntheticMethodForAnnotations.contains(it.simpleName.toString()) }.map { eventFieldElement ->
                    //  @ApiEvent
                    //  public interface ClassName{
                    //      public static final class DefaultImpls {
                    //          @ApiEventField
                    //          public static void get${eventFieldName}$annotations() {
                    //          }
                    //      }
                    //  }
                    val eventFieldName = syntheticMethodForAnnotationsMap[eventFieldElement.simpleName.toString()]!!
                    val eventField = eventFieldElement.getAnnotation(ApiEventField::class.java)
                    val kdoc = elementUtils.getKDoc(eventFieldElement)
                    eventFieldName to (eventField to kdoc)
                }.toTypedArray())
            )
        }


        return if (eventFieldMap == null || eventFieldMap.isEmpty()) {
            GenResult(
                EmptyEventClassName,
                EmptyEventClassName,
                true
            )
        } else {

            genEventClassForKotlin(
                ModuleEventClassName,
                eventElement,
                apiPackageName,
                implPackageName,
                apiFileSpecBuilder,
                apiImplFileSpecBuilder,
                eventFieldMap
            ).also {
                exportApiClassPath.add(it.apiClassName)
            }
        }
    }

    @Suppress("LocalVariableName")
    private fun genEventClassForKotlin(
        ModuleEventClassName: ClassName,
        eventElement: TypeElement,
        apiPackageName: String,
        implPackageName: String,
        apiFileSpecBuilder: FileSpec.Builder,
        apiImplFileSpecBuilder: FileSpec.Builder,
        eventFieldMap: MutableMap<String, Pair<ApiEventField, CodeBlock?>>
    ): GenResult {
        // event类型源
        val eventTypeSpecOrigin = eventElement.toTypeSpec().also {
            check(it.kind == TypeSpec.Kind.INTERFACE) {
                "${eventElement.qualifiedName} must is a interface class."
            }
        }

        val LiveEvent = ClassName(PACKAGE_NAME_EVENT, CLASS_LIVE_EVENT)
        val MutableLiveEvent = ClassName(PACKAGE_NAME_EVENT, CLASS_MUTABLE_LIVE_EVENT)
        val BackgroundLiveEvent = ClassName(PACKAGE_NAME_EVENT, CLASS_BACKGROUND_EVENT)
        val MutableBackgroundLiveEvent = ClassName(PACKAGE_NAME_EVENT, CLASS_MUTABLE_BACKGROUND_EVENT)
        val getEventClassName = { eventOn: EventOn, externalMutable: Boolean ->
            when (eventOn) {
                EventOn.MAIN -> if (!externalMutable) LiveEvent else MutableLiveEvent
                EventOn.BACKGROUND -> if (!externalMutable) BackgroundLiveEvent else MutableBackgroundLiveEvent
            }
        }
        val getDelegateOuterClassName = { eventOn: EventOn ->
            when (eventOn) {
                EventOn.MAIN -> LiveEvent
                EventOn.BACKGROUND -> BackgroundLiveEvent
            }
        }

        val eventClassNameOrigin = eventElement.className()
        val apiName = "${optionModuleName}ModuleEvent"
        val apiClassName = ClassName(apiPackageName, apiName)
        val implClassName = ClassName(implPackageName, "_${apiName}")
        val internalMutableImplClassName = ClassName(implPackageName, "_${apiName}_Mutable")

        val eventOriginPropertySpecs = eventTypeSpecOrigin.propertySpecs // 被注解EventField的所有字段
            .filter { eventFieldMap.containsKey(it.name) }
        val apiPropertySpecsBuilders = eventOriginPropertySpecs.map { // 所有的属性builder
            val (eventField, eventDoc) = eventFieldMap[it.name]!!
            val eventClassName = getEventClassName(eventField.eventOn, eventField.externalMutable)
            PropertySpec.builder(it.name, eventClassName.parameterizedBy(it.type)).apply {
                mutable(false)
                annotations.clear()
                eventDoc?.run(::addKdoc)
                addKdoc("@see %T.%L - origin.", eventClassNameOrigin, it.name)
                addKdoc("\n")
            }
        }

        // Event接口
        val apiPropertySpecs = apiPropertySpecsBuilders.map { it.build() }
        val apiTypeSpec = TypeSpec.interfaceBuilder(apiClassName).run {
            addSuperinterface(ModuleEventClassName)
            addProperties(apiPropertySpecs)
            build()
        }

        // Event实现类
        val implInternalMutableEventPropertyName = "_mutable"
        val implInternalMutableEventProperty : PropertySpec = PropertySpec.builder(implInternalMutableEventPropertyName, internalMutableImplClassName).run {
            addModifiers(KModifier.INTERNAL)
            mutable(false)
            delegate("lazy(LazyThreadSafetyMode.NONE) { %T(this) }", internalMutableImplClassName)
            build()
        }

        val implPropertySpecs = eventOriginPropertySpecs.map {
            val (eventField, _) = eventFieldMap[it.name]!!
            val externalMutable = eventField.externalMutable
            val eventClassName = getEventClassName(eventField.eventOn, eventField.externalMutable)
            val delegateOuterClassName = getDelegateOuterClassName(eventField.eventOn)
            val delegateName = if(externalMutable) CLASS_EVENT_MUTABLE_DELEGATE else CLASS_EVENT_DELEGATE
            PropertySpec.builder(it.name, eventClassName.parameterizedBy(it.type)).run {
                mutable(false)
                addModifiers(KModifier.OVERRIDE)
                delegate(
                    CodeBlock
                        .builder()
                        .add("%T.%L()", delegateOuterClassName, delegateName)
                        .build()
                )
                build()
            }

        }

        val implTypeSpec = apiTypeSpec.toBuilder(TypeSpec.Kind.CLASS, implClassName.simpleName).run {
            addSuperinterface(apiClassName)
            propertySpecs.clear()
            addProperty(implInternalMutableEventProperty)
            addProperties(implPropertySpecs)
            build()
        }

        // 模块内部可变的Event实现类
        val implInternalMutableEventType = TypeSpec.classBuilder(
            name = internalMutableImplClassName.simpleName
        ).run {
            addModifiers(KModifier.INTERNAL)

            // 构造
            val srcPropertyRefName = "real"
            primaryConstructor(FunSpec.constructorBuilder().run {
                addParameter(srcPropertyRefName, implClassName)
                build()
            })
            addProperty(
                PropertySpec.builder(srcPropertyRefName, implClassName).run {
                    initializer(srcPropertyRefName)
                    addModifiers(KModifier.PRIVATE)
                    build()
                }
            )
            addProperty(PropertySpec.builder(srcPropertyRefName, implClassName).run {
                addModifiers(KModifier.PRIVATE)
                mutable(false)
                build()
            })


            // 属性
            addProperties(eventOriginPropertySpecs.map {
                val (eventField, _) = eventFieldMap[it.name]!!
                val eventClassName = getEventClassName(eventField.eventOn, true)
                val delegateOuterClassName = getDelegateOuterClassName(eventField.eventOn)
                PropertySpec.builder(it.name, eventClassName.parameterizedBy(it.type)).run {
                    mutable(false)
                    delegate(
                        "%T.$CLASS_EVENT_INTERNAL_MUTABLE_DELEGATE(%L.%L)",
                        delegateOuterClassName,
                        srcPropertyRefName,
                        it.name
                    )
                    build()
                }

            })
            build()
        }

        // 内部拓展函数
        val implInternalMutableExtFun = FunSpec.builder("mutable")
            .addModifiers(KModifier.INTERNAL)
            .receiver(apiClassName)
            .returns(internalMutableImplClassName)
            .addStatement("return (this as %T).%L", implClassName, implInternalMutableEventPropertyName)
            .build()

        apiFileSpecBuilder.addType(apiTypeSpec.toBuilder().run {
            addKdoc("A event class of $optionModuleName module.\n")
            addKdoc("\n")
            addKdoc("Use `P2M.apiOf(${optionModuleName}).event` to get the instance.\n")
            addKdoc("\n")
            addKdoc("Use `P2M.apiOf(${optionModuleName}).event.mutable()` to get holder instance of mutable event inside the own module.\n")
            addKdoc("\n")
            addKdoc("@see %T - origin.", eventClassNameOrigin)
            build()
        })
        apiImplFileSpecBuilder.addType(implTypeSpec)
        apiImplFileSpecBuilder.addFunction(implInternalMutableExtFun)
        apiImplFileSpecBuilder.addType(implInternalMutableEventType)
        return GenResult(apiClassName, implClassName)
    }
}