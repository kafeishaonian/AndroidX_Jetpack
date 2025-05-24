package com.example.router_plugin

import org.gradle.api.GradleException
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.collections.forEach
import kotlin.text.replace

class AppAsmContextModifier(
    cv: ClassVisitor,
    private val providers: List<ProviderInfo>
) : ClassVisitor(Opcodes.ASM9, cv){

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String?>?
    ): MethodVisitor? {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (name == "initBeanDefinition" && descriptor == "()V") {
            object: MethodVisitor(Opcodes.ASM9, mv) {
                override fun visitCode() {
                    super.visitCode()
                    providers.forEach { info->
                        generateBeanRegistration(this, info)
                    }
                }
            }
        } else {
            mv
        }
    }


    private fun generateBeanRegistration(mv: MethodVisitor, info: ProviderInfo) {
        val interfaceType = Type.getObjectType(info.interfaceType.replace('.', '/'))
        if (interfaceType.sort != Type.OBJECT) {
            throw GradleException("Invalid interface type: ${info.interfaceType}")
        }

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(
            Opcodes.GETFIELD,
            "com/example/router/AppAsmContext",
            "beanDefinitionList",
            "Ljava/util/List;"
        )

        mv.visitTypeInsn(Opcodes.NEW, "com/example/router/TypeBeanDefinition")
        mv.visitInsn(Opcodes.DUP)

        mv.visitLdcInsn(Type.getObjectType(info.interfaceType.replace('.', '/')))
        when {
            info.beanId < 6 -> mv.visitInsn(Opcodes.ICONST_0 + info.beanId)
            info.beanId <= 32767 -> mv.visitIntInsn(Opcodes.SIPUSH, info.beanId)
            else -> mv.visitLdcInsn(info.beanId)
        }

        mv.visitLdcInsn(Type.getObjectType(info.implType.replace('.', '/')))

        //调用构造函数
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "com/example/router/TypeBeanDefinition",
            "<init>",
            "(Ljava/lang/Class;ILjava/lang/Class;)V",
            false
        )

        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/List",
            "add",
            "(Ljava/lang/Object;)Z",
            true
        )
        mv.visitInsn(Opcodes.POP)
    }
}