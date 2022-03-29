package com.p2m.gradle.bean

class LocalModuleProjectUnit extends ModuleProjectUnit {
    boolean runApp                      // 可以运行
    @Override
    String toString() {
        return "module(\"${getModuleName()}\")[Local]"
    }
}
