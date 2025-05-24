package com.example.router_plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

abstract class ProviderCollectorFactory : AsmClassVisitorFactory<ProviderCollectorFactory.Params> {

    interface Params : InstrumentationParameters {

        @get:Input
        val variantName: Property<String>
    }


    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return object : ProviderClassCollector(nextClassVisitor) {

            override fun methodExit(className: String?, interfaces: Array<String>) {
                className?.let { implName ->
                    val interfaceName = interfaces.first()
                    val variantName = parameters.get().variantName.get()
                    synchronized(providersByVariant) {
                        println("Debug 添加数据----------> name: $interfaceName")
                        providersByVariant.getOrPut(variantName) {
                            mutableListOf()
                        }.add(ProviderInfo(interfaceName, implName, 0))
                    }
                }
            }
        }
    }


    override fun isInstrumentable(classData: ClassData): Boolean {
        val result = classData.classAnnotations.any {
            it == "com.example.router.RouterProvider"
        }
        return result
    }


    companion object {
        private val providersByVariant = mutableMapOf<String, MutableList<ProviderInfo>>()

        fun getProviders(variantName: String): List<ProviderInfo> {
            println("Debug 获取数据----------> size: ${providersByVariant[variantName]?.size}")
            return providersByVariant[variantName] ?: emptyList()
        }
    }
}