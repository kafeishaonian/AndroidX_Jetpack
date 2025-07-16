package com.example.router_plugin.track

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode

class AnnotationReader(private val annotation: AnnotationNode) : AnnotationVisitor(Opcodes.ASM9) {
    override fun visit(name: String?, value: Any?) {
        if (annotation.values == null) {
            annotation.values = mutableListOf()
        }
        annotation.values.add(name)
        annotation.values.add(value)
    }

    override fun visitEnum(name: String?, desc: String?, value: String?) {
        if (annotation.values == null) {
            annotation.values = mutableListOf()
        }
        annotation.values.add(name)
        annotation.values.add(value)
    }
}