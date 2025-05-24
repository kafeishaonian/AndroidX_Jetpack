package com.example.router_plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class AppAsmContextModifierFactory: AsmClassVisitorFactory<AppAsmContextModifierFactory.Params> {

    interface Params: InstrumentationParameters {
        @get:Input
        val variantName: Property<String>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        println("Debug isInstrumentable className:= ${classContext.currentClassData.className}")
        return if (classContext.currentClassData.className == "com.example.router.AppAsmContext") {
            val providers = parseProviders()
            AppAsmContextModifier(nextClassVisitor, providers)
        } else {
            nextClassVisitor
        }
    }

    private fun parseProviders(): List<ProviderInfo> {
        val variantName = parameters.get().variantName.get()
        return ProviderCollectorFactory.getProviders(variantName)
            .sortedBy {
                it.implType
            }.mapIndexed { index, info ->
                info.copy(beanId = index + 1)
            }
    }


    override fun isInstrumentable(classData: ClassData): Boolean {
//        println("Debug isInstrumentable className:= ${classData.className}")
        return classData.className == "com.example.router.AppAsmContext"
    }

}