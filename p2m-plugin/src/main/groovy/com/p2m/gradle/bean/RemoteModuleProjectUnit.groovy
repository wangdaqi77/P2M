package com.p2m.gradle.bean

class RemoteModuleProjectUnit extends ModuleProjectUnit {
    @Override
    String toString() {
        return "module(\"${getModuleName()}\")[Remote]"
    }
}
