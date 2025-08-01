package com.example.router_plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.example.router_plugin.bitmap.BitmapClassVisitorFactory
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.objects
        val androidComponents =
            target.extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.beforeVariants { variant ->
            variant.enable = true
        }
        androidComponents.onVariants { variant ->
            val capitalizedVariantName = variant.name.replaceFirstChar { it.uppercase() }

            println("[Debug] 开始执行任务 name:= ${capitalizedVariantName}")

            val taskProvider = target.tasks.register(
                "ProviderCollector${capitalizedVariantName}Task",
                ProviderCollectorTask::class.java
            )
            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                .use(taskProvider)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    ProviderCollectorTask::allJars,
                    ProviderCollectorTask::allDirectories,
                    ProviderCollectorTask::output
                )
        }


        androidComponents.onVariants { variant ->
            variant.instrumentation.transformClassesWith(
                BitmapClassVisitorFactory::class.java,
                InstrumentationScope.PROJECT
            ) {
            }

            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )
        }

//        androidComponents.onVariants { variant ->
//            variant.instrumentation.transformClassesWith(
//                EventClassVisitorConfig::class.java,
//                InstrumentationScope.ALL
//            ) { params ->
//                params.extension.set(variant.buildType)
//            }
//            variant.instrumentation.setAsmFramesComputationMode(
//                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES
//            )
//        }
    }
}