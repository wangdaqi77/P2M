package com.p2m.gradle.bytecode

import com.p2m.gradle.bean.BaseProjectUnit
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

@Deprecated
class ModuleGraphClassAdapter extends ClassVisitor {
    HashMap<String, BaseProjectUnit> p2mProject
    ModuleGraphClassAdapter(ClassVisitor classVisitor, HashMap<String, BaseProjectUnit> p2mProject) {
        super(Opcodes.ASM7, classVisitor)
        this.p2mProject = p2mProject
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (name == "inject") {
            // println("visitMethod -> inject")
            def methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions)
            return new ModuleNameCollectorInjectMethodVisitor(api, methodVisitor, access, name, descriptor, p2mProject)
        }else {
            super.visitMethod(access,name,descriptor,signature,exceptions)
        }
    }
}
