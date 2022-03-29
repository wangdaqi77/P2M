package com.p2m.gradle.bean

class ProjectNamed extends Named{
    public ProjectNamed(String name) {
       super(name)
    }

    @Override
    public String get() {
        return name.startsWith(":") ?
                name.substring(1, name.length()) :
                name
    }

    public getInclude() {
        return name.startsWith(":") ?
                name :
                ":${name}"
    }
}

