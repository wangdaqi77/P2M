package com.p2m.gradle.task

import com.p2m.gradle.utils.Constant
import com.squareup.javawriter.JavaWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import javax.lang.model.element.Modifier

@CacheableTask
abstract class GenerateModuleNameCollector extends DefaultTask {
    public static final String MODULE_AUTO_COLLECTOR = "GeneratedModuleNameCollector"
    public static final String MODULE_AUTO_COLLECTOR_NAME = MODULE_AUTO_COLLECTOR + ".java"
    public static final String MODULE_AUTO_COLLECTOR_SUPER = "com.p2m.core.module.ModuleNameCollector"
    private Property<String> packageName = project.objects.property(String.class)

    @OutputDirectory
    abstract DirectoryProperty getSourceOutputDir()

    @Input
    abstract ListProperty<String> getValidDependenciesName()

    @Input
    Property<String> getPackageName() {
        return packageName
    }

    @TaskAction
    void generate(){
        def packageName = packageName.get()
        File pkgFolder = new File(sourceOutputDir.get().asFile, packageName.replace(".", File.separator))
        if (!pkgFolder.isDirectory() && !pkgFolder.mkdirs()) {
            throw new RuntimeException("Failed to create " + pkgFolder.getAbsolutePath())
        }
        def moduleNameCollectorJavaFile = new File(pkgFolder, MODULE_AUTO_COLLECTOR_NAME)

        FileOutputStream fos = null
        OutputStreamWriter out = null
        JavaWriter writer = null
        try {
            fos = new FileOutputStream(moduleNameCollectorJavaFile)
            out = new OutputStreamWriter(fos, "UTF-8")
            writer = new JavaWriter(out)

            Set classModifiers = new HashSet()
            classModifiers.add(Modifier.PUBLIC)
            classModifiers.add(Modifier.FINAL)
            writer.emitJavadoc(Constant.FILE_GEN_CODE_COMMENT)
                    .emitPackage(packageName)
                    .beginType(MODULE_AUTO_COLLECTOR, "class", classModifiers, MODULE_AUTO_COLLECTOR_SUPER)


            Set constructorModifiers = new HashSet()
            constructorModifiers.add(Modifier.PUBLIC)
            writer.beginConstructor(constructorModifiers)
            validDependenciesName.get().forEach { moduleName ->
                writer.emitStatement("collect(\"%s\")", moduleName)
            }
            writer.endConstructor()
            writer.endType()
        } finally {
            try {writer.close()} catch (Exception e) {}
            try {out.close()} catch (Exception e) {}
            try {fos.close()} catch (Exception e) {}
        }

    }

}
