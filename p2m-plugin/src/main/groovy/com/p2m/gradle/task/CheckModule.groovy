package com.p2m.gradle.task

import com.p2m.gradle.exception.P2MSettingsException
import com.p2m.gradle.utils.Constant
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets

abstract class CheckModule extends DefaultTask {

    @InputFile
    abstract RegularFileProperty getPropertiesFile()

    @TaskAction
    void doCheck(){
        if (propertiesFile.getOrNull() == null) {
            project.logger.error(String.format(Constant.ERROR_MODULE_INIT_NOT_EXIST, project.p2mProject.getModuleName()))
            throw new P2MSettingsException("")
        }

        propertiesFile.get().asFile.newReader(StandardCharsets.UTF_8.name()).eachLine { line ->
            def split = line.split("=")
            def attr = split[0]
            def value = split[1]
            if (attr == "genModuleInitSource") {
                def exist = value.toBoolean()
                if (!exist) {
                    project.logger.error(String.format(Constant.ERROR_MODULE_INIT_NOT_EXIST, project.p2mProject.getModuleName()))
                    throw new P2MSettingsException("")
                }
            }
        }
    }

}
