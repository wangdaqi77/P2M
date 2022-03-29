package com.p2m.gradle

import com.p2m.gradle.bean.BaseProjectUnit
import com.p2m.gradle.bean.ModuleProjectUnit
import com.p2m.gradle.bean.RemoteModuleProjectUnit
import com.p2m.gradle.exception.P2MSettingsException
import com.p2m.gradle.bean.settings.BaseProjectConfig
import com.p2m.gradle.extension.settings.P2MConfig
import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.bean.ModuleNamed
import com.p2m.gradle.bean.settings.ModuleProjectConfig
import com.p2m.gradle.bean.Named
import com.p2m.gradle.utils.Constant
import com.p2m.gradle.utils.NamedUtils
import com.p2m.gradle.utils.ProjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.util.ConfigureUtil

class AndroidP2MPlugin implements Plugin<Settings> {
    private P2MConfig p2mConfig
    private boolean existRunAppModule = false
    private ModuleProjectConfig supportRunAppModuleProjectConfig

    private Map<ModuleNamed, ModuleProjectUnit> moduleProjectUnitTable = new HashMap()
    private Map<ModuleNamed, LocalModuleProjectUnit> localModuleProjectTable = new HashMap<>()
    private Map<ModuleNamed, RemoteModuleProjectUnit> remoteModuleProjectTable = new HashMap<>()

    @Override
    void apply(final Settings settings) {

        p2mConfig = settings.extensions.create("p2m", P2MConfig.class, settings)
        settings.gradle.settingsEvaluated { settings1 ->
            println("======P2M version:${Constant.P2M_VERSION}======")

            loadEnv(settings)

            evaluateModulesFromConfigExtensions()

            startIncludeModulesByConfig(settings)
        }

        settings.gradle.projectsLoaded { gradle ->
            settings.gradle.removeListener(this)

            def rootProject = gradle.rootProject
            rootProject.ext._p2mDevEnv = p2mConfig._devEnv

            genP2MProjectDependencyConfig(rootProject)

            genModuleProjectTable(rootProject)

            rootProject.ext._moduleProjectUnitTable = moduleProjectUnitTable

            configLocalModuleProject()

            configRemoteModuleProject()

            if (!existRunAppModule) {
                configAppProject(rootProject)
            }

            // 为所有apply "com.android.library"插件的项目编译时依赖P2M project
            dependencyP2MProjectForAllApplyAndroidLibraryPluginProject(rootProject)
        }
    }

    private static def checkAppConfig(BaseProjectUnit project) {
        def applicationId = project.project.android.defaultConfig.applicationId
        if (applicationId == null) {
            project.error(
                    """
                        Please configure at the end in ${project.project.projectDir.absolutePath}/build.gradle.
                        p2mRunAppBuildGradle {
                            android {
                                defaultConfig{
                                    applicationId "your value"
                                }
                        
                                sourceSets {
                                    main {
                                        // your src dir for run app.
                                        java.srcDirs += 'src/app/java'
                                        
                                        // your AndroidManifest.xml path for run app.
                                        manifest.srcFile 'src/app/AndroidManifest.xml'
                                    }
                                }
                            }
                        
                            dependencies {
                                // ...
                            }
                        }
                    """
            )
        }
    }

    private def configRemoteModuleProject() {
        remoteModuleProjectTable.values().forEach { RemoteModuleProjectUnit moduleProject -> }
    }

    private def configLocalModuleProject() {
        localModuleProjectTable.values().forEach { LocalModuleProjectUnit moduleProject ->
            moduleProject.project.ext.runApp = moduleProject.runApp
            moduleProject.project.ext.p2mProject = moduleProject
            moduleProject.project.ext._p2mMavenRepositoryClosure = p2mConfig._p2mMavenRepositoryClosure
        }

        localModuleProjectTable.values().forEach { LocalModuleProjectUnit moduleProject ->
            moduleProject.project.beforeEvaluate {
                moduleProject.project.plugins.apply(moduleProject.isApp() ? Constant.PLUGIN_ID_ANDROID_APP : Constant.PLUGIN_ID_ANDROID_LIBRARY)
                moduleProject.project.plugins.apply(Constant.PLUGIN_ID_KOTLIN_ANDROID)
                moduleProject.project.plugins.apply(Constant.PLUGIN_ID_KOTLIN_KAPT)
                moduleProject.project.plugins.apply(ModuleWhenRunAppConfigPlugin)
                moduleProject.project.plugins.apply(ProductApiPlugin)
            }
        }

        localModuleProjectTable.values().forEach { LocalModuleProjectUnit moduleProject ->
            moduleProject.project.beforeEvaluate {
                if (moduleProject.isApp()) {
                    moduleProject.project.plugins.apply(ModuleRunAppProjectPlugin)
                    // checkAppConflictWhenEvaluated(moduleProject)
                    moduleProject.project.afterEvaluate {
                        checkAppConfig(moduleProject)
                    }
                } else {
                    def mavenPublishPlugin = moduleProject.project.plugins.findPlugin(Constant.PLUGIN_ID_MAVEN_PUBLISH)
                    if (mavenPublishPlugin == null) {
                        moduleProject.project.plugins.apply(Constant.PLUGIN_ID_MAVEN_PUBLISH)
                    }
                    moduleProject.project.plugins.apply(LocalModuleProjectPlugin)
                }
            }

        }
    }


    private def configAppProject(Project rootProject) {
        p2mConfig.appProjectConfigs.forEach { appProjectConfig ->
            def appProject = createMainAppProject(rootProject, appProjectConfig, moduleProjectUnitTable)
            appProject.project.ext.p2mProject = appProject
            appProject.project.ext._p2mMavenRepositoryClosure = p2mConfig._p2mMavenRepositoryClosure

            appProject.project.beforeEvaluate {
                appProject.project.plugins.apply(Constant.PLUGIN_ID_ANDROID_APP)
                appProject.project.plugins.apply(Constant.PLUGIN_ID_KOTLIN_ANDROID)
                appProject.project.plugins.apply(Constant.PLUGIN_ID_KOTLIN_KAPT)
            }

            appProject.project.afterEvaluate {
                appProject.project.plugins.apply(MainAppProjectPlugin)
            }
            // checkAppConflictWhenEvaluated(appProject)
        }
    }

    private static def dependencyP2MProjectForAllApplyAndroidLibraryPluginProject(Project rootProject) {
        rootProject.allprojects.each { project ->
            project.afterEvaluate {
                if (project.name != Constant.P2M_NAME_API && project.plugins.hasPlugin(Constant.PLUGIN_ID_ANDROID_LIBRARY)) {
                    project.dependencies.add("compileOnly", project._p2mApi())
                }
            }
        }
    }

    private def genP2MProjectDependencyConfig(Project rootProject) {
        rootProject.allprojects {
            repositories {
                if (p2mConfig._useLocalRepoForP2MProject) {
                    mavenLocal()
                }
                maven { url Constant.REPO_P2M_REMOTE }
            }
        }

        if (p2mConfig._devEnv) {
            rootProject.ext._p2mApi = { return rootProject.project(":${Constant.P2M_NAME_API}") }
            rootProject.ext._p2mAnnotation = { return rootProject.project(":${Constant.P2M_NAME_ANNOTATION}") }
            rootProject.ext._p2mCompiler = { return rootProject.project(":${Constant.P2M_NAME_COMPILER}") }
        } else {
            rootProject.ext._p2mApi = { return "${Constant.P2M_GROUP_ID}:${Constant.P2M_NAME_API}:${Constant.P2M_VERSION}" }
            rootProject.ext._p2mAnnotation = { return "${Constant.P2M_GROUP_ID}:${Constant.P2M_NAME_ANNOTATION}:${Constant.P2M_VERSION}" }
            rootProject.ext._p2mCompiler = { return "${Constant.P2M_GROUP_ID}:${Constant.P2M_NAME_COMPILER}:${Constant.P2M_VERSION}" }
        }
    }

    private def genModuleProjectTable(Project rootProject) {
        moduleProjectUnitTable.clear()
        localModuleProjectTable.clear()
        remoteModuleProjectTable.clear()
        Map<ModuleProjectUnit, Set<ModuleNamed>> moduleProjectConfigDependencies = new HashMap()

        def projects = new HashSet<Project>()
        projects.addAll(rootProject.subprojects)
        def moduleConfigs = p2mConfig.modulesConfig.values()
        def iterator = moduleConfigs.iterator()

        // for remote module aar
        while (iterator.hasNext()) {
            def moduleConfig = iterator.next()
            if (moduleConfig.useRemote) { // 创建remote module
                def moduleProject = ProjectFactory.createRemoteModuleProject(moduleConfig)
                moduleProjectConfigDependencies.put(moduleProject, moduleConfig._dependencyContainer._dependencies)
                remoteModuleProjectTable[moduleConfig._moduleNamed] = moduleProject
                moduleProjectUnitTable.put(moduleConfig._moduleNamed, moduleProject)
                moduleProject.project = rootProject
                iterator.remove()
            }
        }

        // for local module
        projects.forEach { Project project ->
            iterator = moduleConfigs.iterator()
            while (iterator.hasNext()) {
                def moduleConfig = iterator.next()
                if (!moduleConfig.useRemote && NamedUtils.project(project.name) == moduleConfig._projectNamed) {
                    def moduleProject = ProjectFactory.createLocalModuleProject(moduleConfig)
                    moduleProject.project = rootProject.project(moduleConfig._projectNamed.include)
                    moduleProjectConfigDependencies.put(moduleProject, moduleConfig._dependencyContainer._dependencies)
                    localModuleProjectTable[moduleConfig._moduleNamed] = moduleProject
                    moduleProjectUnitTable.put(moduleConfig._moduleNamed, moduleProject)
                    iterator.remove()
                }
            }
        }


        if (!moduleConfigs.empty) {
            rootProject.logger.warn("genModuleProjectTable fail count is ${moduleConfigs.size()}.", new IllegalStateException())
        }

        // 建立module依赖关系
        moduleProjectConfigDependencies.forEach { ModuleProjectUnit moduleProject, Set<ModuleNamed> dependencies ->
            configDependenciesToProjectDependencies(moduleProject, dependencies, moduleProjectUnitTable)
        }
    }

    private def createMainAppProject(Project rootProject, appProjectConfig, Map<ModuleNamed, ModuleProjectUnit> moduleProjectTable) {
        def iterator = rootProject.subprojects.iterator()
        while (iterator.hasNext()) {
            def project = iterator.next()
            if (appProjectConfig._projectNamed == NamedUtils.project(project.name)) {
                def appProject = ProjectFactory.createMainAppProject(appProjectConfig)
                appProject.project = project
                configDependenciesToProjectDependencies(appProject, appProjectConfig._dependencyContainer._dependencies, moduleProjectTable)
                return appProject
            }
        }

        throw new P2MSettingsException("UnKnow Error.")
    }


    private def configDependenciesToProjectDependencies = { BaseProjectUnit ownerProject, ownerConfigDependencies, Map<ModuleNamed, ModuleProjectUnit> moduleTable ->
        if (ownerConfigDependencies == null) return
        ownerConfigDependencies.forEach { ModuleNamed dependencyModuleNamed ->
            if (moduleTable[dependencyModuleNamed] == null) {
                ownerProject.error("Non-existent ${NamedUtils.getStatement(dependencyModuleNamed)} dependency, Please check ${NamedUtils.getStatement(ownerProject.moduleNamed)} in settings.gradle.")
            }
            println("p2m: ${ownerProject} depends on ${moduleTable[dependencyModuleNamed]}")
            if (ownerProject.moduleNamed == dependencyModuleNamed) {
                ownerProject.error("Unsupport depends on self! Please check ${NamedUtils.getStatement(ownerProject.moduleNamed)} in settings.gradle.")
            }
            ownerProject.dependencies.add(moduleTable[dependencyModuleNamed])
        }
    }


    private static ProjectDescriptor includeProject(Settings settings, BaseProjectConfig projectConfig) {
        settings.include(projectConfig._projectPath)
        if (projectConfig._projectDescriptorClosure != null) {
            def projectDescriptor = settings.project(projectConfig._projectPath)
            if (projectConfig._projectDirPath != null) {
                String dirName = projectConfig._projectDirPath.replace(":", "")
                projectDescriptor.projectDir = new File("./$dirName")
            }

            ConfigureUtil.configure(projectConfig._projectDescriptorClosure, projectDescriptor)
        }
        return settings.project(projectConfig._projectPath)
    }


    private def startIncludeModulesByConfig(Settings settings) {
        if (!existRunAppModule) {
            // include app壳
            p2mConfig.appProjectConfigs.forEach { appProjectConfig ->
                def projectDescriptor = includeProject(settings, appProjectConfig)
                println("p2m: include App-${projectDescriptor.name}, path:${projectDescriptor.projectDir.absolutePath}")
            }
        }

        // include modules
        p2mConfig.modulesConfig.forEach { key, moduleConfig ->
            if (moduleConfig.useRemote) {
                println("p2m: include module(\"${moduleConfig._moduleNamed.get()}\"), Remote groupId:${moduleConfig.getGroupId()} artifactId:${moduleConfig.getArtifactId()} versionName:${moduleConfig.getVersionName()}")
            } else {
                def projectDescriptor = includeProject(settings, moduleConfig)
                println("p2m: include module(\"${moduleConfig._moduleNamed.get()}\"), Local path:${projectDescriptor.projectDir.absolutePath}")
            }
        }
    }

    private def loadEnv(Settings settings) {
        def isDevEnv = false
        def useLocalRepoForP2MProject = false
        def localProperties = new Properties()
        def localPropertiesFile = new File(settings.rootDir, "local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile.withReader('UTF-8') { reader ->
                localProperties.load(reader)
            }

            if (localProperties.getProperty(Constant.LOCAL_PROPERTY_DEV_ENV) != null) {
                isDevEnv = localProperties.getProperty(Constant.LOCAL_PROPERTY_DEV_ENV).toBoolean()
            }

            if (localProperties.getProperty(Constant.LOCAL_PROPERTY_USE_LOCAL_REPO_FOR_P2M_PROJECT) != null) {
                useLocalRepoForP2MProject = localProperties.getProperty(Constant.LOCAL_PROPERTY_USE_LOCAL_REPO_FOR_P2M_PROJECT).toBoolean()
            }
        }

        if (isDevEnv) {
            settings.include(":${Constant.P2M_PROJECT_NAME_P2M_CORE}")
            settings.project(":${Constant.P2M_PROJECT_NAME_P2M_CORE}").projectDir = new File("../${Constant.P2M_PROJECT_NAME_P2M_CORE}")
            settings.include(":${Constant.P2M_PROJECT_NAME_P2M_COMPILER}")
            settings.project(":${Constant.P2M_PROJECT_NAME_P2M_COMPILER}").projectDir = new File("../${Constant.P2M_PROJECT_NAME_P2M_COMPILER}")
            settings.include(":${Constant.P2M_PROJECT_NAME_P2M_ANNOTATION}")
            settings.project(":${Constant.P2M_PROJECT_NAME_P2M_ANNOTATION}").projectDir = new File("../${Constant.P2M_PROJECT_NAME_P2M_ANNOTATION}")
        }
        p2mConfig._devEnv = isDevEnv
        p2mConfig._useLocalRepoForP2MProject = useLocalRepoForP2MProject
    }

    private def evaluateModulesFromConfigExtensions() {
        def appProjectConfigs = p2mConfig.appProjectConfigs
        if (appProjectConfigs == null || appProjectConfigs.empty) {
            throw new P2MSettingsException("Please add p2m config content in settings.gradle, ex:\n" +
                    "p2m {\n" +
                    "   app {\n" +
                    "       include(\":{your project path}\") {\n" +
                    "           projectDir = new File(\"{your project path}\")\n" +
                    "       }\n" +
                    "       dependencies {\n" +
                    "           ${NamedUtils.getStatement(new ModuleNamed("{YourModuleName 1}"))}\n" +
                    "           ${NamedUtils.getStatement(new ModuleNamed("{YourModuleName 2}"))}\n" +
                    "           ... \n" +
                    "       }\n" +
                    "   }\n" +
                    "   module(\"{YourModuleName}\") {\n" +
                    "       include(\":{your module project path}\") {\n" +
                    "           projectDir = new File(\"{your module project path}\")\n" +
                    "       }\n" +
                    "       dependencies {\n" +
                    "           ${NamedUtils.getStatement(new ModuleNamed("{YourModuleName 1}"))}\n" +
                    "           ${NamedUtils.getStatement(new ModuleNamed("{YourModuleName 2}"))}\n" +
                    "           ... \n" +
                    "       }\n" +
                    "   }\n" +
                    "}\n" +
                    "\n"
            )
        }

        appProjectConfigs.forEach { appProjectConfig ->
            appProjectConfig.runAppConfig.enabled = false
        }

        p2mConfig.modulesConfig.forEach { Named named, ModuleProjectConfig moduleConfig ->
            if (moduleConfig.runApp) {

                if (moduleConfig.useRemote) {
                    throw new P2MSettingsException("""
                            ${NamedUtils.getStatement(moduleConfig._moduleNamed)} setted runApp=true already, it can no longer set repo=true.
                            Please check config in settings.gradle.
                        """
                    )
                }

                if (supportRunAppModuleProjectConfig != null) {
                    throw new P2MSettingsException("""
                            ${NamedUtils.getStatement(supportRunAppModuleProjectConfig._moduleNamed)} setted runApp=true already, only one module can set runApp=true.
                            Please check config in settings.gradle.
                        """
                    )
                }
                supportRunAppModuleProjectConfig = moduleConfig
                existRunAppModule = true
            }

        }

        if (existRunAppModule) {
            reevaluateModulesIfModuleRunApp(supportRunAppModuleProjectConfig)
        }

    }

    private Set<ModuleNamed> collectAllDependencies(ModuleProjectConfig moduleProjectConfig) {
        Set<ModuleNamed> allDependencies = new HashSet<ModuleNamed>()
        allDependencies.add(moduleProjectConfig._moduleNamed)
        def dependencies = moduleProjectConfig._dependencyContainer._dependencies
        if (dependencies != null) {
            dependencies.forEach { moduleNamed ->
                allDependencies.addAll(collectAllDependencies(p2mConfig.modulesConfig[moduleNamed]))
            }
        }
        return allDependencies
    }

    private def reevaluateModulesIfModuleRunApp(ModuleProjectConfig supportRunAppModuleProjectConfig) {
        Set<ModuleNamed> allDependencies = collectAllDependencies(supportRunAppModuleProjectConfig)
        Set<ModuleNamed> willRemove = new HashSet<ModuleNamed>()
        willRemove.addAll(p2mConfig.modulesConfig.keySet())
        willRemove.removeAll(allDependencies)
        willRemove.forEach { remove ->
            p2mConfig.modulesConfig.remove(remove)
        }
    }
}
