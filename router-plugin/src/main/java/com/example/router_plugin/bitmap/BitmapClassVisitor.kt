package com.example.router_plugin.bitmap

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class BitmapClassVisitor(
    cv: ClassVisitor
) : ClassVisitor(Opcodes.ASM9, cv){

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)

        return if ((access and Opcodes.ACC_NATIVE) == 0 &&
            (access and Opcodes.ACC_ABSTRACT) == 0) {
            BitmapMethodVisitor(mv, access, name, descriptor)
        } else {
            mv
        }
    }

}