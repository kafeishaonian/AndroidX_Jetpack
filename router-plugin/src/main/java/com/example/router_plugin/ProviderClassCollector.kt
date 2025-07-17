package com.example.router_plugin

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import kotlin.collections.isNotEmpty
import kotlin.text.replace

abstract class ProviderClassCollector(
    nextClassVisitor: ClassVisitor,
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    private var className: String? = null
    private var hasRouterProvider = false
    private var hasEventModule = false
    private var interfaces = emptyArray<String>()
    private var isConcreteClass = false

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String?>?
    ) {
        className = name?.replace('/', '.') ?: ""
        this.interfaces = interfaces?.map {
            it?.replace('/', '.') ?: ""
        }?.toTypedArray() ?: emptyArray()
        isConcreteClass =
            (access and Opcodes.ACC_INTERFACE) == 0 && (access and Opcodes.ACC_ABSTRACT) == 0
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if (descriptor == "Lcom/example/router/RouterProvider;") {
            hasRouterProvider = true
        }
        if (descriptor == "Lcom/example/router/log/annotations/EventModule;") {
            hasEventModule = true
        }
        return super.visitAnnotation(descriptor, visible)
    }


    override fun visitEnd() {
        if (hasRouterProvider && isConcreteClass && interfaces.isNotEmpty()) {
            methodExit(className, interfaces)
        }
        if (hasEventModule && className?.isNotEmpty() == true) {
            methodEdit(className, "${className}Impl.class")
        }
        super.visitEnd()
    }

    abstract fun methodExit(className: String?, interfaces: Array<String>)

    abstract fun methodEdit(className: String?, targetName: String)
}