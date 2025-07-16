package com.example.router_plugin.track

import com.android.build.api.instrumentation.ClassContext
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import java.io.FileOutputStream
import java.nio.file.Paths


class EventClassVisitor(
    nextVisitor: ClassVisitor,
    private val classContext: ClassContext
) : ClassVisitor(Opcodes.ASM9, nextVisitor) {

    private var moduleName: String? = null
    private var className: String? = null
    private var isInterface = false
    private val methods = mutableListOf<MethodData>()

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
        isInterface = access and Opcodes.ACC_INTERFACE != 0
    }


    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if (descriptor == "Lcom/example/router/log/annotations/EventModule;") {
            return object : AnnotationVisitor(Opcodes.ASM9) {
                override fun visit(name: String?, value: Any?) {
                    if ("value" == name) moduleName = value.toString()
                }
            }
        }
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val original = super.visitMethod(access, name, descriptor, signature, exceptions)

        // 如果是接口的抽象方法
        if (isInterface && (access and Opcodes.ACC_ABSTRACT) != 0) {
            val methodData = MethodData(
                access, name, descriptor, signature, exceptions?.toList()
            )
            methods.add(methodData)

            return object : MethodVisitor(Opcodes.ASM9, original) {
                private var clickEvent: AnnotationNode? = null
                private var showEvent: AnnotationNode? = null
                private val paramAnnotations = mutableMapOf<Int, MutableList<AnnotationNode>>()

                override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor {
                    when (desc) {
                        "Lcom/example/router/log/annotations/ClickEvent;" -> {
                            clickEvent = AnnotationNode(desc)
                            methodData.clickEvent = clickEvent
                            return AnnotationReader(clickEvent!!)
                        }
                        "Lcom/example/router/log/annotations/ShowEvent;" -> {
                            showEvent = AnnotationNode(desc)
                            methodData.showEvent = showEvent
                            return AnnotationReader(showEvent!!)
                        }
                    }
                    return super.visitAnnotation(desc, visible)
                }

                override fun visitParameterAnnotation(
                    parameter: Int,
                    desc: String?,
                    visible: Boolean
                ): AnnotationVisitor {
                    val list = paramAnnotations.getOrPut(parameter) { mutableListOf() }
                    val annotation = AnnotationNode(desc)
                    list.add(annotation)
                    return AnnotationReader(annotation)
                }

                override fun visitEnd() {
                    methodData.paramAnnotations = paramAnnotations
                    super.visitEnd()
                }
            }
        }
        return original
    }

    override fun visitEnd() {
        if (isInterface && moduleName != null && className != null && methods.isNotEmpty()) {
            val implName = "${className}Impl"
            val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
            generateImplClass(implName, cw)

            super.visitEnd()

            val byteCode = cw.toByteArray()
            saveClassToFile(byteCode, implName)

        } else {
            super.visitEnd()
        }
    }


    private fun saveClassToFile(bytecode: ByteArray, implName: String) {
        val outputPath: String = Paths.get("$implName.class").toString()
        try {
            FileOutputStream(outputPath).use { fos ->
                fos.write(bytecode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun generateImplClass(implName: String, cw: ClassWriter) {
        cw.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC,
            implName,
            null,
            "java/lang/Object",
            arrayOf(className)
        )
        // 添加 @Generated 注解
        val av = cw.visitAnnotation("Ljavax/annotation/processing/Generated;", true)
        av.visit("value", "com.example.EventPlugin")
        av.visitEnd()

        // 生成构造函数
        generateConstructor(cw, implName)

        methods.forEach { methodData ->
            when {
                methodData.clickEvent != null -> generateEventMethod(
                    cw,
                    methodData,
                    methodData.clickEvent!!,
                    moduleName!!,
                    "click"
                )

                methodData.showEvent != null -> generateEventMethod(
                    cw,
                    methodData,
                    methodData.showEvent!!,
                    moduleName!!,
                    "show"
                )
            }
        }
        cw.visitEnd()
    }

    private fun generateConstructor(cw: ClassWriter, className: String) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        )
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }


    private fun generateEventMethod(
        cw: ClassWriter,
        methodData: MethodData,
        eventAnnotation: AnnotationNode,
        moduleName: String,
        type: String
    ) {
        // 解析注解值（正确的处理方式）
        val eventId = findAnnotationValue(eventAnnotation, "eventId") ?: return
        val page = findAnnotationValue(eventAnnotation, "page") ?: return
        val action = findAnnotationValue(eventAnnotation, "action") ?: return

        val mv = cw.visitMethod(
            methodData.access and Opcodes.ACC_ABSTRACT.inv() or Opcodes.ACC_PUBLIC,
            methodData.name,
            methodData.descriptor,
            methodData.signature,
            methodData.exceptions?.toTypedArray()
        )

        mv.visitCode()

        // 创建参数Map
        val paramTypes = Type.getArgumentTypes(methodData.descriptor)
        val mapIndex = calculateMapIndex(paramTypes)

        // 创建 HashMap
        mv.visitTypeInsn(Opcodes.NEW, "java/util/HashMap")
        mv.visitInsn(Opcodes.DUP)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false)
        mv.visitVarInsn(Opcodes.ASTORE, mapIndex)

        // 处理参数注解
        methodData.paramAnnotations.forEach { (paramIndex, annotations) ->
            val varIndex = getParamVarIndex(paramIndex, paramTypes)

            annotations.forEach { annotation ->
                when (annotation.desc) {
                    "Lcom/example/router/log/annotations/Param;" -> {
                        val key = findAnnotationValue(annotation, "key") ?: return@forEach

                        mv.visitVarInsn(Opcodes.ALOAD, mapIndex)
                        mv.visitLdcInsn(key)
                        loadParameter(mv, varIndex, paramTypes[paramIndex])
                        mv.visitMethodInsn(
                            Opcodes.INVOKEINTERFACE,
                            "java/util/Map",
                            "put",
                            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                            true
                        )
                        mv.visitInsn(Opcodes.POP)
                    }
                    "Lcom/example/router/log/annotations/ParamMap;" -> {
                        mv.visitVarInsn(Opcodes.ALOAD, mapIndex)
                        mv.visitVarInsn(Opcodes.ALOAD, varIndex)
                        mv.visitMethodInsn(
                            Opcodes.INVOKEINTERFACE,
                            "java/util/Map",
                            "putAll",
                            "(Ljava/util/Map;)V",
                            true
                        )
                    }
                }
            }
        }

        // 创建 EventData 对象
        mv.visitTypeInsn(Opcodes.NEW, "com/example/router/log/EventData")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(moduleName)
        mv.visitLdcInsn(type)
        mv.visitLdcInsn(eventId)
        mv.visitLdcInsn(page)
        mv.visitLdcInsn(action)
        mv.visitVarInsn(Opcodes.ALOAD, mapIndex)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "com/example/router/log/EventData",
            "<init>",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;J)V",
            false
        )

        // 调用 EventCache.addEvent()
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "com/example/router/log/EventCache",
            "addEvent",
            "(Lcom/example/router/log/EventData;)V",
            false
        )

        // 根据返回类型处理返回语句
        val returnType = Type.getReturnType(methodData.descriptor)
        if (returnType.sort == Type.VOID) {
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(7, mapIndex + 1) // 估计的栈大小
        } else {
            // 返回默认值
            when (returnType.sort) {
                Type.BOOLEAN -> mv.visitInsn(Opcodes.ICONST_0)
                Type.BYTE -> mv.visitInsn(Opcodes.ICONST_0)
                Type.CHAR -> mv.visitInsn(Opcodes.ICONST_0)
                Type.SHORT -> mv.visitInsn(Opcodes.ICONST_0)
                Type.INT -> mv.visitInsn(Opcodes.ICONST_0)
                Type.LONG -> mv.visitInsn(Opcodes.LCONST_0)
                Type.FLOAT -> mv.visitInsn(Opcodes.FCONST_0)
                Type.DOUBLE -> mv.visitInsn(Opcodes.DCONST_0)
                else -> mv.visitInsn(Opcodes.ACONST_NULL)
            }
            mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN))
            mv.visitMaxs(7, mapIndex + 1) // 估计的栈大小
        }

        mv.visitEnd()
    }

    private fun findAnnotationValue(annotation: AnnotationNode, name: String): String? {
        if (annotation.values == null) return null

        var i = 0
        while (i < annotation.values.size) {
            if (annotation.values[i] == name) {
                return annotation.values[i + 1] as? String
            }
            i += 2
        }
        return null
    }

    private fun calculateMapIndex(paramTypes: Array<Type>): Int {
        var index = 1 // 非静态方法的 this 引用在索引 0
        paramTypes.forEach {
            index += it.size
        }
        return index
    }

    private fun getParamVarIndex(paramIndex: Int, paramTypes: Array<Type>): Int {
        var index = 1 // 非静态方法的 this 引用在索引 0
        for (i in 0 until paramIndex) {
            index += paramTypes[i].size
        }
        return index
    }

    private fun loadParameter(mv: MethodVisitor, varIndex: Int, type: Type) {
        when (type.sort) {
            Type.BOOLEAN -> mv.visitVarInsn(Opcodes.ILOAD, varIndex)
            Type.BYTE -> mv.visitVarInsn(Opcodes.ILOAD, varIndex)
            Type.CHAR -> mv.visitVarInsn(Opcodes.ILOAD, varIndex)
            Type.SHORT -> mv.visitVarInsn(Opcodes.ILOAD, varIndex)
            Type.INT -> mv.visitVarInsn(Opcodes.ILOAD, varIndex)
            Type.LONG -> mv.visitVarInsn(Opcodes.LLOAD, varIndex)
            Type.FLOAT -> mv.visitVarInsn(Opcodes.FLOAD, varIndex)
            Type.DOUBLE -> mv.visitVarInsn(Opcodes.DLOAD, varIndex)
            else -> mv.visitVarInsn(Opcodes.ALOAD, varIndex)
        }
    }
}