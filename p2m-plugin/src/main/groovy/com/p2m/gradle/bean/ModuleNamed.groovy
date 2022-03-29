package com.p2m.gradle.bean


class ModuleNamed extends Named {
    public ModuleNamed(String name) {
        super(name)
    }

    @Override
    public String get() {
        return name
    }
}

