package com.p2m.gradle

import com.p2m.gradle.bean.LocalModuleProjectUnit
import com.p2m.gradle.utils.LastProcessManifestRegister
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * For local module
 */
class LocalModuleProjectPlugin extends BaseSupportDependencyModulePlugin {
    private LocalModuleProjectUnit moduleProject

    @Override
    void doAction(Project project) {
        super.doAction(project)
        moduleProject = project.p2mProject

        project.dependencies { DependencyHandler handler ->
            handler.add("compileOnly", project._p2mApi())
            handler.add("compileOnly", project._p2mAnnotation())
            handler.add("kapt", project._p2mCompiler())
//            handler.add("kapt", "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0") {
//                exclude group: "org.jetbrains.kotlin", module: "kotlin-stdlib"
//            }
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
    }
}