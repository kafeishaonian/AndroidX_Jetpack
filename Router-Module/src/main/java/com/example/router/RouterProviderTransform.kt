//package com.example.router
//
//import com.example.router_plugin.AppAsmContextModifier
//import com.example.router_plugin.ProviderClassCollector
//import com.example.router_plugin.ProviderInfo
//
//
//class RouterProviderTransform : Transform() {
//
//    override fun process(context: TransformContext) {
//        // 第一阶段：收集所有被注解的类
//        val providers = mutableListOf<ProviderInfo>().apply {
//            context.inputs.forEach { transformInput ->
//                transformInput.directoryInputs.forEach { dirInput ->
//                    dirInput.file.walk().filter { it.isFile && it.name.endsWith(".class") }.forEach { file ->
//                        ClassReader(file.readBytes()).accept(
//                            ProviderClassCollector().also { it ->
//                                ClassReader(file.readBytes()).accept(it, ClassReader.SKIP_DEBUG)
//                            },
//                            ClassReader.SKIP_DEBUG
//                        )
//                        addAll(it.providers)
//                    }
//                }
//            }
//        }.sortedBy { it.implType }
//
//        // 分配自增ID
//        providers.forEachIndexed { index, info ->
//            info.beanId = index + 1
//        }
//
//        // 第二阶段：修改目标类
//        context.inputs.forEach { transformInput ->
//            transformInput.directoryInputs.forEach { dirInput ->
//                dirInput.file.walk().filter { it.isFile && it.name.endsWith(".class") }.forEach { file ->
//                    if (file.name == "AppAsmContext.class") {
//                        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
//                        ClassReader(file.readBytes()).accept(
//                            AppAsmContextModifier(writer, providers),
//                            0
//                        )
//                        file.writeBytes(writer.toByteArray())
//                    }
//                }
//            }
//        }
//    }
//}