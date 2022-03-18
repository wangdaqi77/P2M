package com.p2m.gradle.utils

import com.android.Version
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Action
import org.gradle.api.Project

class AndroidUtils {

    static def whenEvaluated = { Project project, Action<Project> action->
        if (project.state.executed) {
            action.execute(project)
        }else {
            project.afterEvaluate {
                action.execute(project)
            }
        }
    }

    static def forAppEachVariant = {Project project, Action<BaseVariant> action->
        project.android.applicationVariants.all(action)
    }
    static def forLibraryEachVariant = {Project project, Action<BaseVariant> action->
        project.android.libraryVariants.all(action)
    }

    static def forEachVariant = {Project project, Action<BaseVariant> action->
        def androidExtension = project.extensions.getByName("android")
        if (androidExtension instanceof AppExtension) {
            androidExtension.applicationVariants.all(action)
        }else if (androidExtension instanceof LibraryExtension){
            androidExtension.libraryVariants.all(action)
        }else if (androidExtension instanceof TestExtension){
            androidExtension.applicationVariants.all(action)
        }

        if (androidExtension instanceof TestedExtension) {
            androidExtension.testVariants.all(action)
            androidExtension.unitTestVariants.all(action)
        }
    }

    static def getApplicationIdByVariant = { BaseVariant variant->
        return isAGP7 ? [variant.mergedFlavor.applicationId, variant.buildType.applicationIdSuffix].findAll().join() : variant.getApplicationId()
    }

    static def isAGP7 = Version.ANDROID_GRADLE_PLUGIN_VERSION.startsWith("7")
}
