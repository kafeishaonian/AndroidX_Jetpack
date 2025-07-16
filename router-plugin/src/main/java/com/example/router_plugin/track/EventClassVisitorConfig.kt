package com.example.router_plugin.track

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor

abstract class EventClassVisitorConfig : AsmClassVisitorFactory<EventClassVisitorConfig.Params> {

    interface Params : InstrumentationParameters {

        @get:Input
        @get:Optional
        val extension: Property<String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return EventClassVisitor(nextClassVisitor, classContext)
    }


    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.classAnnotations.any { it == "Lcom/example/router/log/annotations/EventModule;" }
    }

}