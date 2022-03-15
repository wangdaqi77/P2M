package com.p2m.gradle

import com.android.build.gradle.api.BaseVariant
import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.bean.ModuleProjectUnit
import com.p2m.gradle.bean.RemoteModuleProjectUnit
import com.p2m.gradle.utils.AndroidUtils
import com.p2m.gradle.utils.Constant
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.util.ConfigureUtil

abstract class BaseSupportDependencyModulePlugin extends BaseModulePlugin {

    @Override
    void doAction(Project project) {
        project.repositories.maven { MavenArtifactRepository repository ->
            if (project._p2mMavenRepositoryClosure != null) {
                ConfigureUtil.configure(project._p2mMavenRepositoryClosure, repository)
            }
            if (repository.getUrl() == null) {
                repository.url = project.rootProject.file("repo")
            }
            repository.name = Constant.P2M_MODULE_AAR_REPO_NAME
        }

        project.afterEvaluate {
            project.dependencies { DependencyHandler dependencyHandler ->
                moduleDependencies.forEach { ModuleProjectUnit moduleProject ->
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
    }

    def remoteDependsOn = { DependencyHandler dependencyHandler, ModuleProjectUnit moduleProject ->
        dependencyHandler.add("runtimeOnly", "${moduleProject.groupId}:${moduleProject.moduleArtifactId}:${moduleProject.versionName}")
        dependencyHandler.add("compileOnly", "${moduleProject.groupId}:${moduleProject.apiArtifactId}:${moduleProject.versionName}")
    }

    def localDependsOn = { Project project, DependencyHandler dependencyHandler, ModuleProjectUnit moduleProject ->
        dependencyHandler.add("runtimeOnly", moduleProject.project)
        AndroidUtils.forEachVariant(project) { BaseVariant variant ->
            dependencyHandler.add("${variant.buildType.name}CompileOnly", dependencyHandler.project(path: moduleProject.project.path, configuration: "$variant.buildType.name$Constant.P2M_CONFIGURATION_NAME_SUFFIX_MODULE_API"))
        }
    }
}
