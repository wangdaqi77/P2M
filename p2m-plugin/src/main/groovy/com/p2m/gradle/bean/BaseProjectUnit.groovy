package com.p2m.gradle.bean

import org.gradle.api.Project

abstract class BaseProjectUnit {
    Named moduleNamed
    Project project                                         // 项目
    Set<ModuleProjectUnit> dependencies = new HashSet()     // 依赖的模块
    RunAppConfig runAppConfig

    boolean isApp(){
        if (this instanceof AppProjectUnit) return true
        if (this instanceof RemoteModuleProjectUnit) return false
        if (this instanceof LocalModuleProjectUnit) {
            return ((LocalModuleProjectUnit)this).runApp
        }
        return false
    }

    String getModuleName() {
        return moduleNamed.get()
    }

    void error(String message) {
        message = "P2M: $message"
        if (project != null) {
            project.logger.error(message)
        }
        throw P2MSettingsException(message)
    }

    void info(String message) {
        message = "P2M: $message"
        if (project != null) {
            project.logger.info(message)
        }
    }


    @Override
    int hashCode() {
        return moduleNamed.hashCode()
    }

    @Override
    boolean equals(Object o) {
        if (!o instanceof BaseProjectUnit) {
            return false
        }
        return moduleNamed.equals((o as BaseProjectUnit).moduleNamed)
    }
}
