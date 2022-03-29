package com.p2m.gradle.utils

import com.android.build.gradle.internal.api.ApkVariantImpl
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.android.build.gradle.tasks.ProcessApplicationManifest
import com.p2m.gradle.bean.BaseProjectUnit
import com.p2m.gradle.bean.RunAppConfig
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import org.codehaus.groovy.runtime.InvokerHelper

class RunAppConfigUtils {
    static def modifyMergedManifestXmlForRunApp = { BaseProjectUnit projectUnit, RunAppConfig runAppConfig->
        def project = projectUnit.project
        AndroidUtils.forAppEachVariant(project) {  variant->
            variant.getPreBuildProvider().get().doFirst {
                if ("debug" != variant.buildType.name) {
                    projectUnit.error(" Property runApp=true only support debug mode. Please check that in settings.gradle.")
                }
            }
            variant.outputs.each {output ->
                ManifestProcessorTask manifestProcessorTask = output.processManifestProvider.get()
                // ProcessApplicationManifest
                if (manifestProcessorTask instanceof ProcessApplicationManifest) {
                    manifestProcessorTask.doLast {
                        def manifestOutputDirectory = manifestProcessorTask.manifestOutputDirectory
                        manifestOutputDirectory.asFileTree.each { File file->
                            // 清单文件
                            if (file.name == Constant.FILE_NAME_ANDROID_MANIFEST_XML){
                                println("${manifestProcessorTask.path} -> ${file.absolutePath}")
                                // 属于本项目的build文件夹
                                if (file.absolutePath.startsWith(project.buildDir.absolutePath)) {
                                    def manifestFile = file
                                    def newManifestFile = new File(file.absolutePath)
                                    def manifestXmlParser = new XmlSlurper().parse(manifestFile)

                                    RunAppConfigUtils.applyMainActivity(manifestXmlParser, runAppConfig)
                                    RunAppConfigUtils.applyAutoInitializer(manifestXmlParser)

                                    Object builder = Class.forName("groovy.xml.StreamingMarkupBuilder").getDeclaredConstructor().newInstance()
                                    InvokerHelper.setProperty(builder, "encoding", "UTF-8")
                                    // Writable w = (Writable)InvokerHelper.invokeMethod(builder, "bindNode", manifestXmlParser)
                                    // println("${w.toString()}")
                                    newManifestFile.withWriter {out->
                                        XmlUtil.serialize(manifestXmlParser, out)
                                    }

                                }
                            }
                        }
                    }
                }
            }
            if (variant instanceof ApkVariantImpl) {
                variant.processJavaResources
            }
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
            println("P2M open auto initialization")
            application.appendNode{
                provider(
                        "android:authorities": "${applicationId}.auto-initialize",
                        "android:name": "com.p2m.core.internal.module.alone.run.AutoInitializerForDebug",
                        "android:exported": "false",
                )
            }
    }
}
