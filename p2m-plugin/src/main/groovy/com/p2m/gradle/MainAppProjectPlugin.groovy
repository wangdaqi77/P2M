package com.p2m.gradle

import com.p2m.gradle.bean.AppProjectUnit
import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.bean.ModuleProjectUnit
import com.p2m.gradle.bean.RemoteModuleProjectUnit
import com.p2m.gradle.utils.GenerateModuleNameCollectorJavaTaskRegister
import com.p2m.gradle.utils.RunAppConfigUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * For app
 */
class MainAppProjectPlugin extends BaseSupportDependencyModulePlugin {
    AppProjectUnit appProject

    @Override
    void doAction(Project project) {
        super.doAction(project)
        appProject = projectUnit as AppProjectUnit
        project.android {
            if (runAppConfig.enabled) {
                defaultConfig {
                    applicationId = appProject.runAppConfig.applicationId
                    versionCode = runAppConfig.versionCode
                    versionName = runAppConfig.versionName
                }

                RunAppConfigUtils.modifyMergedManifestXmlForRunApp(appProject, runAppConfig)
            }
        }

        project.dependencies { DependencyHandler dependencyHandler ->
            dependencyHandler.add("implementation", project._p2mApi())
            dependencyHandler.add("implementation", project._p2mAnnotation())

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

        GenerateModuleNameCollectorJavaTaskRegister.register(appProject, false)
    }

}
