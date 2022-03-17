package com.p2m.gradle.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.bundling.Jar

abstract class ApiJar extends Jar {

    @InputDirectory
    abstract DirectoryProperty getInputKaptDir()

    @InputFiles
    abstract ConfigurableFileCollection getInputKotlinCompilerSource()

}
