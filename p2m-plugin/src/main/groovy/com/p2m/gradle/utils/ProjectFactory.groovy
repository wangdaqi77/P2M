package com.p2m.gradle.utils

import com.p2m.gradle.bean.AppProjectUnit
import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.bean.RemoteModuleProjectUnit
import com.p2m.gradle.bean.settings.AppProjectConfig
import com.p2m.gradle.bean.settings.ModuleProjectConfig

class ProjectFactory {

    static LocalModuleProjectUnit createLocalModuleProject(ModuleProjectConfig moduleConfig) {
        def moduleProject = new LocalModuleProjectUnit()
        moduleProject.moduleNamed = moduleConfig._moduleNamed
        moduleProject.groupId = moduleConfig.getGroupId()
        moduleProject.artifactId = moduleConfig.getArtifactId()
        moduleProject.apiArtifactId = moduleConfig.getApiArtifactId()
        moduleProject.versionName = moduleConfig.getVersionName()
        moduleProject.runApp = moduleConfig.runApp
        moduleProject.runAppConfig = moduleConfig.runAppConfig
        return moduleProject
    }

    static RemoteModuleProjectUnit createRemoteModuleProject(ModuleProjectConfig moduleConfig) {
        def moduleProject = new RemoteModuleProjectUnit()
        moduleProject.moduleNamed = moduleConfig._moduleNamed
        moduleProject.groupId = moduleConfig.getGroupId()
        moduleProject.artifactId = moduleConfig.getArtifactId()
        moduleProject.apiArtifactId = moduleConfig.getApiArtifactId()
        moduleProject.versionName = moduleConfig.getVersionName()
        return moduleProject
    }
    
    static AppProjectUnit createMainAppProject(AppProjectConfig config) {
        def appProject = new AppProjectUnit()
        appProject.moduleNamed = config._moduleNamed
        appProject.runAppConfig = config.runAppConfig
        return appProject
    }


}
