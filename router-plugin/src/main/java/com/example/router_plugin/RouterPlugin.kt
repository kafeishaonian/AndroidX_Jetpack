package com.example.router_plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val androidComponents =
            target.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val capitalizedVariantName = variant.name.replaceFirstChar { it.uppercase() }

            println("[Debug] 开始执行任务 name:= ${capitalizedVariantName}")

            val taskProvider = target.tasks.register("ProviderCollector${capitalizedVariantName}Task",
                ProviderCollectorTask::class.java)
            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                .use(taskProvider)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    ProviderCollectorTask::allJars,
                    ProviderCollectorTask::allDirectories,
                    ProviderCollectorTask::output
                )
        }
    }
}