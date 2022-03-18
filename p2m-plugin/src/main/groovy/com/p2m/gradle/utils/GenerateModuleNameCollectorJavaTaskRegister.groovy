package com.p2m.gradle.utils

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.p2m.gradle.bean.BaseProjectUnit
import com.p2m.gradle.bean.ModuleProjectUnit
import com.p2m.gradle.task.GenerateModuleNameCollector
import org.gradle.api.tasks.TaskProvider

import java.util.concurrent.Callable

class GenerateModuleNameCollectorJavaTaskRegister {

    public static void register(BaseProjectUnit moduleProject, boolean collectSelf){

        HashSet<ModuleProjectUnit> validDependencies = ModuleProjectUtils.collectValidDependencies(moduleProject, collectSelf)
        def project = moduleProject.project
        AndroidUtils.forEachVariant(project) { BaseVariant variant ->
            variant = variant as ApplicationVariant
            def variantName = variant.name
            def variantTaskMiddleName = variantName.capitalize()
            def taskName = "generate${variantTaskMiddleName}GeneratedModuleNameCollector"
            HashSet<String> validDependenciesNames = new HashSet<String>()
            validDependencies.forEach { validDependenciesNames.add(it.moduleName) }
            def generateModuleNameCollectorSourceOutputDir = new File(
                    project.getBuildDir().absolutePath
                            + File.separator
                            + "generated"
                            + File.separator
                            + "source"
                            + File.separator
                            + "GeneratedModuleNameCollector"
                            + File.separator
                            + variantName)
            TaskProvider taskProvider = project.tasks.register(taskName, GenerateModuleNameCollector.class) {
                dependsOn variant.getApplicationIdTextResource()
                sourceOutputDir.set(generateModuleNameCollectorSourceOutputDir)
                packageName.set(project.providers.provider(new Callable<String>() {
                    @Override
                     String call() throws Exception {
                        return variant.getApplicationIdTextResource().asString()
                    }
                }))
                validDependenciesName.set(validDependenciesNames)
            }

            variant.registerJavaGeneratingTask(taskProvider.get(), generateModuleNameCollectorSourceOutputDir)
            variant.addJavaSourceFoldersToModel(generateModuleNameCollectorSourceOutputDir)
        }
    }


}
