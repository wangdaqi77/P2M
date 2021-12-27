package com.p2m.gradle

import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.bean.ModuleProjectUnit
import com.p2m.gradle.bean.RemoteModuleProjectUnit
import com.p2m.gradle.utils.GenerateModuleNameCollectorJavaTaskRegister
import com.p2m.gradle.utils.LastProcessManifestRegister
import com.p2m.gradle.utils.RunAppConfigUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * For run app of local module.
 */
class ModuleRunAppProjectPlugin extends BaseSupportDependencyModulePlugin {
    LocalModuleProjectUnit moduleProject

    @Override
    void doAction(Project project) {
        super.doAction(project)
        moduleProject = project.p2mProject
        if (runAppConfig.enabled) {
            project.android {
                defaultConfig {
                    applicationId = moduleProject.runAppConfig.applicationId
                    versionCode = runAppConfig.versionCode
                    versionName = runAppConfig.versionName
                }
            }
            RunAppConfigUtils.modifyMergedManifestXmlForRunApp(moduleProject, runAppConfig)
        }
        project.dependencies { DependencyHandler dependencyHandler ->
            project._moduleProjectUnitTable.values().forEach { ModuleProjectUnit moduleProject ->
                if (moduleProject.project != project) {
                    if (moduleProject instanceof RemoteModuleProjectUnit) {
                        remoteDependsOn(dependencyHandler, moduleProject)
                    }

                    if (moduleProject instanceof LocalModuleProjectUnit) {
                        if (moduleProject.project.state.executed) {
                            localDependsOn(project, dependencyHandler, moduleProject)
                        } else {
                            moduleProject.project.afterEvaluate {
                                localDependsOn(project, dependencyHandler, moduleProject)
                            }
                        }
                    }
                }
            }
        }
        project.dependencies { DependencyHandler handler ->
            handler.add("implementation", project._p2mApi())
            handler.add("implementation", project._p2mAnnotation())
            handler.add("kapt", project._p2mCompiler())
        }

        def lastProcessManifestRegister = new LastProcessManifestRegister(moduleProject)
        lastProcessManifestRegister.register { Node topNode->
            //noinspection GroovyAccessibility
            Node application = topNode.getByName("application")[0]
            def applicationId = topNode.attribute("package")
            Map<String, String> attributes = new HashMap()
            attributes.put("xmlns:android", "http://schemas.android.com/apk/res/android")
            attributes.put(
                    "android:name",
                    "p2m:module=${moduleProject.moduleName}"
            )
            attributes.put(
                    "android:value",
                    "" +
                            "publicModuleClass=${applicationId}.p2m.api.${moduleProject.moduleName}" +
                            "," +
                            "implModuleClass=${applicationId}.p2m.impl._${moduleProject.moduleName}"
            )
            application.appendNode('meta-data', attributes)
        }

        GenerateModuleNameCollectorJavaTaskRegister.register(moduleProject, true)
    }
}