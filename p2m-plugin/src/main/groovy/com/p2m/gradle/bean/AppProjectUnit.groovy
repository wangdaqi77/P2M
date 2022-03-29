package com.p2m.gradle.bean

class AppProjectUnit extends BaseProjectUnit {
    @Override
    String toString() {
        return "${getModuleName()}-${project.name}"
    }
}
