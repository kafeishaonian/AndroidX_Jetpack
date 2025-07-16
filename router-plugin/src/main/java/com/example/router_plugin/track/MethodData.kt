package com.example.router_plugin.track

import org.objectweb.asm.tree.AnnotationNode

data class MethodData(
    val access: Int,
    val name: String?,
    val descriptor: String?,
    val signature: String?,
    val exceptions: List<String>?,
) {
    var clickEvent: AnnotationNode? = null
    var showEvent: AnnotationNode? = null
    var paramAnnotations: Map<Int, List<AnnotationNode>> = emptyMap()
}