//package com.example.router_plugin.track
//
//import com.android.build.api.instrumentation.AsmClassVisitorFactory
//import com.android.build.api.instrumentation.ClassContext
//import com.android.build.api.instrumentation.ClassData
//import com.android.build.api.instrumentation.InstrumentationParameters
//import org.gradle.api.provider.Property
//import org.gradle.api.tasks.Input
//import org.objectweb.asm.ClassVisitor
//
//abstract class EventClassVisitorConfig : AsmClassVisitorFactory<EventClassVisitorConfig.Params> {
//
//    interface Params : InstrumentationParameters {
//
//        @get:Input
//        val extension: Property<String>
//    }
//
//    override fun createClassVisitor(
//        classContext: ClassContext,
//        nextClassVisitor: ClassVisitor
//    ): ClassVisitor {
//        val extensionValue = parameters.get().extension.get()
//        return EventClassVisitor(nextClassVisitor, extensionValue)
//    }
//
//
//    override fun isInstrumentable(classData: ClassData): Boolean {
//        return classData.classAnnotations.any {
//            it == "com.example.router.log.annotations.EventModule"
//        }
//    }
//
//}