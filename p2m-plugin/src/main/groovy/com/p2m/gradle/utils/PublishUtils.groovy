package com.p2m.gradle.utils

import com.android.build.gradle.api.BaseVariant
import com.p2m.gradle.bean.LocalModuleProjectUnit
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.internal.KaptTask

class PublishUtils {

    static def createPublish = { LocalModuleProjectUnit moduleProject->
        def project = moduleProject.project

        project.publishing.repositories.maven{ MavenArtifactRepository repository->
            if (project._p2mMavenRepositoryClosure != null) {
                ConfigureUtil.configure(project._p2mMavenRepositoryClosure, repository)
            }
            if (repository.getUrl() == null) {
                repository.url = project.rootProject.file("repo")
            }
            repository.name = Constant.P2M_MODULE_AAR_REPO_NAME
        }

        AndroidUtils.forEachVariant(moduleProject.project) { BaseVariant variant ->
            if (variant.name != variant.buildType.name) return
            if (variant.buildType.name != "release") return
            def buildTypeName = variant.buildType.name
            def variantTaskMiddleName = buildTypeName.capitalize()
            TaskProvider<Task> bundleAarTaskProvider
            Provider<KaptTask> kaptKotlin
            try {
                bundleAarTaskProvider = project.tasks.named("bundle${variantTaskMiddleName}Aar")
                kaptKotlin = project.tasks.named("kapt${variantTaskMiddleName}Kotlin")
            } catch (UnknownTaskException e) {
                project.logger.debug(e.cause)
                return
            }

            TaskProvider<Task> publishAllModuleTaskProvider
            try {
                publishAllModuleTaskProvider = moduleProject.project.rootProject.tasks.named("publishAllModule")
            } catch (UnknownTaskException e) {
                publishAllModuleTaskProvider = moduleProject.project.rootProject.tasks.register("publishAllModule")
            }


            def sourcesJar = project.tasks.register("sourcesJarForPublishModule", Jar.class) {
                dependsOn(kaptKotlin)
                group = Constant.P2M_PUBLISH_TASK_GROUP
                archiveFileName.set("${moduleProject.moduleName}-module-sources.jar")
                archiveClassifier.set(project.provider { 'sources' })
                from project.android.sourceSets.main.java.srcDirs
                from kaptKotlin.get().destinationDir
            }

            project.publishing {
                publications {
                    "p2mModule"(MavenPublication) {
                        version moduleProject.versionName
                        setGroupId moduleProject.groupId
                        setArtifactId moduleProject.moduleArtifactId
                        artifact bundleAarTaskProvider.get()
                        artifact sourcesJar.get()
                    }

                    "p2mModuleApi"(MavenPublication) {
                        version moduleProject.versionName
                        setGroupId moduleProject.groupId
                        setArtifactId moduleProject.apiArtifactId
                        artifact project.tasks.named("${Constant.P2M_TASK_NAME_PREFIX_COMPILE_MODULE_API}${variantTaskMiddleName}").get()
                        artifact project.tasks.named("${Constant.P2M_TASK_NAME_PREFIX_COMPILE_MODULE_API_SOURCE}${variantTaskMiddleName}").get()

                        if (!project._p2mDevEnv) {
                            pom.withXml {
                                def dependenciesNode = asNode().appendNode('dependencies')
                                def dependencyNode = dependenciesNode.appendNode('dependency')
                                dependencyNode.appendNode('groupId', Constant.P2M_GROUP_ID)
                                dependencyNode.appendNode('artifactId', Constant.P2M_NAME_API)
                                dependencyNode.appendNode('version', Constant.P2M_VERSION)

                            }
                        }
                    }
                }
            }

            def publishTaskName = "publish${moduleProject.getModuleName()}"
            def publishModuleTaskName = "publishP2mModulePublicationToP2mMavenRepositoryRepository"
            def publishModuleApiTaskName = "publishP2mModuleApiPublicationToP2mMavenRepositoryRepository"
            def publishTaskProvider = moduleProject.project.rootProject.tasks.register(publishTaskName) { task ->
                task.group = Constant.P2M_PUBLISH_TASK_GROUP
                task.description = "publish ${moduleProject.getModuleName()} module"
                task.dependsOn(moduleProject.project.tasks.named(publishModuleTaskName))
                task.dependsOn(moduleProject.project.tasks.named(publishModuleApiTaskName))
            }

            publishAllModuleTaskProvider.configure { task ->
                task.group = Constant.P2M_PUBLISH_TASK_GROUP
                if (task.description == null) {
                    task.description = "publish all module."
                }
                task.dependsOn(publishTaskProvider)
            }
        }

    }
}
