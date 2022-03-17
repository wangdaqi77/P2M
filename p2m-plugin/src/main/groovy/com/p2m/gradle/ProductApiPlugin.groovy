package com.p2m.gradle

import com.android.build.gradle.api.BaseVariant
import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.bean.ModuleProjectUnit
import com.p2m.gradle.task.ApiJar
import com.p2m.gradle.task.ApiSourceJar
import com.p2m.gradle.task.CheckModule
import com.p2m.gradle.utils.AndroidUtils
import com.p2m.gradle.utils.Constant
import com.p2m.gradle.utils.ModuleProjectUtils
import com.p2m.gradle.utils.PublishUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.internal.KaptTask

import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable

class ProductApiPlugin implements Plugin<Project> {
    private LocalModuleProjectUnit moduleProject

    @Override
    void apply(Project project) {
        moduleProject = project.p2mProject

        ModuleProjectUtils.maybeCreateModuleApiConfiguration(moduleProject)

        moduleProject.project.kapt.arguments {
            if (options == null) return

            StringBuffer sb = new StringBuffer()
            moduleProject.dependencies.forEach{ ModuleProjectUnit dependency ->
                String unit = ",${dependency.getModuleName()}"
                sb.append(unit)
            }

            if (sb.length() != 0) {
                sb.deleteCharAt(0)
            }

            arg("moduleName", moduleProject.getModuleName())
            arg("dependencies", sb.toString())
        }

        AndroidUtils.forEachVariant(project) { BaseVariant variant ->
            moduleProject.project.kapt.arguments {
                arg("applicationId", variant.getApplicationId())
            }
        }

        project.afterEvaluate {
            productModuleApi(project)
            if (!moduleProject.isApp()) {
                PublishUtils.createPublish(moduleProject)
            }
        }
    }

    private def productModuleApi = { Project project ->

        AndroidUtils.forEachVariant(project) { BaseVariant variant ->
            def variantName = variant.name
            def variantTaskMiddleName = variantName.capitalize()
            Provider<AbstractCompile> compileKotlin
            Provider<KaptTask> kaptKotlin
            try{
                project.tasks.named("bundle${variantTaskMiddleName}Aar")
                compileKotlin = project.tasks.named("compile${variantTaskMiddleName}Kotlin")
                kaptKotlin = project.tasks.named("kapt${variantTaskMiddleName}Kotlin")
            }catch( UnknownTaskException e){
                project.logger.debug(e.cause)
                return
            }

            def apiJarName = "p2m-${moduleProject.getModuleName()}-module-api.jar"
            def apiSourceJarName = "p2m-${moduleProject.getModuleName()}-module-api-sources.jar"
            def p2mApiJarDir = new File(project.buildDir, "generated/p2m/jar/${variantName}")
            def p2mKaptSrcDir = new File(project.buildDir, "generated/source/kapt/${variantName}")
            def p2mApiPropertiesFile = new File(p2mKaptSrcDir, "p2m_module_api.properties")
            def p2mApiPropertiesConfigurableFileCollection = project.files(p2mApiPropertiesFile)
            p2mApiPropertiesConfigurableFileCollection.builtBy(kaptKotlin)

            def checkModuleProvider = project.tasks.register("${Constant.P2M_TASK_NAME_PREFIX_CHECK_MODULE}${variantTaskMiddleName}", CheckModule.class)
            def compileApiProvider = project.tasks.register("${Constant.P2M_TASK_NAME_PREFIX_COMPILE_MODULE_API}${variantTaskMiddleName}", ApiJar.class)
            def compileApiSourceProvider = project.tasks.register("${Constant.P2M_TASK_NAME_PREFIX_COMPILE_MODULE_API_SOURCE}${variantTaskMiddleName}", ApiSourceJar.class)

            checkModuleProvider.configure {
                group = Constant.P2M_MODULE_TASK_GROUP
                description = "check ${moduleProject.getModuleName()} module for ${variantName}"
                dependsOn(kaptKotlin)
                propertiesFile.set(p2mApiPropertiesConfigurableFileCollection.getSingleFile())
            }

            compileApiProvider.configure {
                group = Constant.P2M_MODULE_TASK_GROUP
                description = "compile api classes of ${moduleProject.getModuleName()} for ${variantName}"
                dependsOn(kaptKotlin)
                dependsOn(checkModuleProvider)
                dependsOn(compileKotlin)
                archiveFileName.set(apiJarName)
                destinationDirectory.set(p2mApiJarDir)
                inputKaptDir.set(kaptKotlin.get().destinationDir)// for UP-TO-DATE
                inputKotlinCompilerSource.from(compileKotlin.get().source) // for UP-TO-DATE
                // value like java/lang/String
                List<String> exportApiClassPathList = new ArrayList<>()

                doFirst {

                    p2mApiPropertiesConfigurableFileCollection.getSingleFile().newReader(StandardCharsets.UTF_8.name()).eachLine { line ->
                        def split = line.split("=")
                        def attr = split[0]
                        def value = split[1]
                        if (attr == "exportApiClassPath") {
                            exportApiClassPathList.addAll(value.split(","))
                        }
                    }

                    from project.files(compileKotlin.get().outputs) {
                        exportApiClassPathList.forEach{ name->
                            project.logger.info("${moduleProject.getModuleName()}:${compileApiProvider.get().name} include $name")
                            include "${name}.*"
                        }

                    }

                }

            }

            compileApiSourceProvider.configure{
                group = Constant.P2M_MODULE_TASK_GROUP
                description = "compile ${moduleProject.getModuleName()} module api source for ${variantName}"
                dependsOn(kaptKotlin)
                dependsOn(checkModuleProvider)
                archiveFileName.set(apiSourceJarName)
                destinationDirectory.set(p2mApiJarDir)
                archiveClassifier.set('sources')
                // java/lang/String
                List<String> exportApiSourcePathList = new ArrayList<>()
                inputKaptDir.set(kaptKotlin.get().destinationDir) // for UP-TO-DATE
                inputKotlinCompilerSource.from(compileKotlin.get().source) // for UP-TO-DATE
                doFirst {
                    p2mApiPropertiesConfigurableFileCollection.getSingleFile().newReader(StandardCharsets.UTF_8.name()).eachLine { line ->
                        def split = line.split("=")
                        def attr = split[0]
                        def value = split[1]
                        if (attr == "exportApiSourcePath") {
                            exportApiSourcePathList.addAll(value.split(","))
                        }
                    }

                    from project.files(kaptKotlin.get().outputs) {
                        exportApiSourcePathList.forEach{ name->
                            project.logger.info("${moduleProject.getModuleName()}:${compileApiSourceProvider.get().name} include $name")
                            include "${name}.*"
                        }

                    }
                }
            }

            variant.javaCompileProvider.configure {
                dependsOn(checkModuleProvider, compileApiProvider, compileApiSourceProvider)
            }

            project.artifacts {
                "$variantName$Constant.P2M_CONFIGURATION_NAME_SUFFIX_MODULE_API"(compileApiProvider)
                "$variantName$Constant.P2M_CONFIGURATION_NAME_SUFFIX_MODULE_API"(compileApiSourceProvider)
            }
        }

    }
}