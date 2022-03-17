package com.p2m.gradle.utils


import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.android.build.gradle.tasks.ProcessApplicationManifest
import com.android.build.gradle.tasks.ProcessLibraryManifest
import com.p2m.gradle.bean.BaseProjectUnit
import com.p2m.gradle.bean.RunAppConfig
import groovy.io.GroovyPrintWriter
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.ConfigureUtil

class LastProcessManifestRegister {

    BaseProjectUnit projectUnit
    ArrayList<Closure<Node>> callbacks = new ArrayList()
    boolean immutable = false

    LastProcessManifestRegister(BaseProjectUnit projectUnit){
        this.projectUnit = projectUnit
        hookTask(projectUnit)
    }

    void register(Closure<Node> closure){
        if (immutable) {
            projectUnit.error("Cannot register at this stage.")
            return
        }
        callbacks.add(closure)
    }

    private def hookTask = { BaseProjectUnit projectUnit ->
        def project = projectUnit.project
        def action = { BaseVariant variant ->
            variant.outputs.each { output ->
                TaskProvider processManifestProvider = output.processManifestProvider
                processManifestProvider.configure { ManifestProcessorTask manifestProcessorTask ->
                    manifestProcessorTask.doLast {
                        immutable = true
                        if (callbacks.isEmpty()) {
                            return
                        }
                        File file = null
                        if (manifestProcessorTask instanceof ProcessApplicationManifest) {
                            def manifestOutputDirectory = manifestProcessorTask.manifestOutputDirectory
                            manifestOutputDirectory.asFileTree.each {
                                if (it.name == Constant.FILE_NAME_ANDROID_MANIFEST_XML) {
                                    file = it
                                }
                            }
                        } else if (manifestProcessorTask instanceof ProcessLibraryManifest) {
                            file = manifestProcessorTask.manifestOutputFile.asFile.get()
                        }
                        // print("hook file: ${file.absolutePath}")
                        if (file != null) {
                            if (file.absolutePath.startsWith(project.buildDir.absolutePath)) {
                                def manifestFile = file
                                def newManifestFile = new File(file.absolutePath)
                                def topNode = new XmlParser().parse(manifestFile)
                                callbacks.each {
                                    ConfigureUtil.configure(it, topNode)
                                }

                                newManifestFile.withWriter { out ->
                                    //XmlUtil.serialize(topNode, out)
                                    Writer sw = new StringWriter();
                                    GroovyPrintWriter pw = new GroovyPrintWriter(sw);
                                    XmlNodePrinter nodePrinter = new XmlNodePrinter(pw);
                                    nodePrinter.setPreserveWhitespace(true);
                                    nodePrinter.print(topNode);
                                    out.write(sw.toString())
                                }

                            }
                        }
                    }
                }
            }
        }
        if (project.plugins.findPlugin(Constant.PLUGIN_ID_ANDROID_APP)) {
            AndroidUtils.forAppEachVariant(project, action)
        } else {
            AndroidUtils.forLibraryEachVariant(project, action)
        }
    }


    static def applyMainActivity = { GPathResult manifestXmlParser, RunAppConfig runAppConfig->
        def application = manifestXmlParser.application[0]

        def index = -1
        def curIndex = -1
        def hasIntentFilter = false
        def hasMainAction = false
        def hasLauncherCategory = false
        application.activity.each {
            curIndex++
            if (it.@"android:name" == runAppConfig.mainActivity) {
                index = curIndex
                it.children().each {
                    if(it.name() == "intent-filter"){
                        hasIntentFilter = true
                        it.children().each{
                            if(it.name()=="action" && it.@"android:name"=="android.intent.action.MAIN"){
                                hasMainAction = true
                            }

                            if(it.name()=="category" && it.@"android:name"=="android.intent.category.LAUNCHER"){
                                hasLauncherCategory = true
                            }
                        }
                    }
                }
            }else {
                it.children().each {
                    if(it.name() == "intent-filter"){
                        it.children().each{
                            if(it.name()=="action" && it.@"android:name"=="android.intent.action.MAIN"){
                                it.replaceNode { }
                            }

                            if(it.name()=="category" && it.@"android:name"=="android.intent.category.LAUNCHER"){
                                hasLauncherCategory = true
                            }
                        }
                    }
                }
            }
        }
        if (index == -1) {
            throw new IllegalArgumentException(
                    "\nNot found ${runAppConfig.mainActivity}, Please check mainActivity=${runAppConfig.mainActivity} in settings.gradle, or config ${runAppConfig.mainActivity} by append activity node in ${Constant.FILE_NAME_ANDROID_MANIFEST_XML}."
            )
        }

        if (!hasIntentFilter) {
            application.activity[index].appendNode {
                'intent-filter' {
                    action('android:name': "android.intent.action.MAIN")
                    if (!hasLauncherCategory) {
                        category('android:name': "android.intent.category.LAUNCHER")
                    }
                }
            }
        }

        application.activity[index].children().each {
            if (it.name() == "intent-filter") {
                if (!hasMainAction) {
                    it.appendNode{
                        action('android:name': "android.intent.action.MAIN")
                    }
                }

                if (!hasLauncherCategory) {
                    it.appendNode{
                        category('android:name': "android.intent.category.LAUNCHER")
                    }
                }
            }
        }
    }

    /**
     * 如果不存在application，则自动初始化
     */
    static def applyAutoInitializer = { GPathResult manifestXmlParser ->
        def application = manifestXmlParser.application[0]
        def applicationId= manifestXmlParser.@"package"
//        def applicationName = application.@"android:name"
//        String applicationNameValue = "${applicationName}"
//        if (applicationNameValue.isEmpty()) {
            println("P2M open auto initialization")
            def providerIndex = application.children().size()
            // println("providerIndex = ${providerIndex}")
            application.appendNode{
                provider(
                        "android:authorities": "${applicationId}.auto-initialize",
                        "android:name": "com.p2m.core.internal.module.alone.run.AutoInitializerForDebugModule",
                        "android:exported": "false",
                )
                //"provider".@"android:authorities"="com.p2m.core.auto-initialize"()
            }
            // application.children().getAt(providerIndex).attributes().put()
//            def providerNodeChild = application.children().getAt(providerIndex).getAt(0)
//            println("providerNodeChild = ${providerNodeChild}")
//            providerNodeChild.@"android:authorities" = "com.p2m.core.auto-initializer"
//            providerNodeChild.@"android:name" = "com.p2m.core.internal.module.alone.run.AutoInitializer"
//            providerNodeChild.@"android:exported" = "false"
//        }
    }
}
