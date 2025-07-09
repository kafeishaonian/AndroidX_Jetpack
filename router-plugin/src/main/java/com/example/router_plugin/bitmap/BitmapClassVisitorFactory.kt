package com.example.router_plugin.bitmap

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class BitmapClassVisitorFactory : AsmClassVisitorFactory<BitmapClassVisitorFactory.Params> {

    interface Params : InstrumentationParameters {

        @get:Input
        val extension: Property<String>
    }


    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return BitmapClassVisitor(nextClassVisitor)
    }
}