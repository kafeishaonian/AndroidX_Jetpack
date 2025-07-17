package com.example.router_plugin

import com.example.router_plugin.track.EventClassVisitor
import com.example.router_plugin.track.EventJarInfo
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

internal const val ROUTER_INJECT = "com/example/router/AppAsmContext.class"

private val blackList = arrayOf(
    "androidx/",
    "android/",
    "kotlin/",
    "kotlinx/",
    "com/google/",
    "org/"
)

internal abstract class ProviderCollectorTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allJars: ListProperty<RegularFile>

    //    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val addOperateList = CopyOnWriteArrayList<ProviderInfo>()
        val addEventList = CopyOnWriteArrayList<EventJarInfo>()

        var targetOperate: File? = null

        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))

        allDirectories.get().onEach { directory ->
            val directoryUri = directory.asFile.toURI()
            directory.asFile
                .walk()
                .filter { it.isFile }
                .forEach { file ->
                    val filePath = directoryUri
                        .relativize(file.toURI())
                        .path
                        .replace(File.separatorChar, '/')

                    if (filePath == ROUTER_INJECT) {
                        targetOperate = file
                        return@forEach
                    }

                    jarOutput.putNextEntry(JarEntry(filePath))
                    file.inputStream().use {
                        it.copyTo(jarOutput)
                    }
                    jarOutput.closeEntry()

                    if (file.name.endsWith(".class")) {
//                        val className = file.name.removeSuffix(".class")
//                        if (!className.isModuleClass()) {
//                            return@forEach
//                        }
                        file.inputStream().use {
                            val classReader = ClassReader(it)
                            classReader.accept(
                                acceptProviderClassCollector(addOperateList, addEventList, file),
                                ClassReader.SKIP_DEBUG
                            )
                        }
                    }
                }
        }
        allJars.get().onEach { file ->
            val jarFile = JarFile(file.asFile)
            jarFile.entries().iterator().forEach { jarEntry ->
                try {
                    if (jarEntry.name == ROUTER_INJECT) {
                        targetOperate = file.asFile
                        return@forEach
                    }

                    jarOutput.putNextEntry(JarEntry(jarEntry.name))
                    jarFile.getInputStream(jarEntry).use {
                        it.copyTo(jarOutput)
                    }

                    val have = blackList.any {
                        jarEntry.name.startsWith(it)
                    }

                    if (!have && jarEntry.name.endsWith(".class")) {
//                        val className = jarEntry.name.removeSuffix(".class")
//                        if (!className.isModuleClass()) {
//                            return@forEach
//                        }
                        jarFile.getInputStream(jarEntry).use {
                            val classReader = ClassReader(it)
                            classReader.accept(
                                acceptProviderClassCollector(addOperateList, addEventList, file.asFile),
                                ClassReader.SKIP_DEBUG
                            )
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    jarOutput.closeEntry()
                }
            }
            jarFile.close()
        }
        if (targetOperate == null) {
            throw RuntimeException("not found $ROUTER_INJECT")
        }

        val jarFile = JarFile(targetOperate)
        jarOutput.putNextEntry(JarEntry(ROUTER_INJECT))
        jarFile.getInputStream(jarFile.getJarEntry(ROUTER_INJECT)).use {
            val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
            val appAsmVisitor = AppAsmContextModifier(writer, parseProviders(addOperateList))
            ClassReader(it).accept(appAsmVisitor, ClassReader.SKIP_DEBUG)
            jarOutput.write(writer.toByteArray())
            jarOutput.closeEntry()
        }
        if (addEventList.isNotEmpty()) {
            addEventList.forEach { eventModule->
                val eventJarFile = JarFile(eventModule.jarFile)
                val className = eventModule.className
                val classEntryPath = "${className.replace('.', '/')}.class"

                val implBytes = with(eventJarFile.getInputStream(eventJarFile.getJarEntry(classEntryPath))) {
                    use { stream->
                        val classReader = ClassReader(stream.readBytes())
                        val writer = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
                        val eventVisitor = EventClassVisitor(writer)

                        classReader.accept(eventVisitor, ClassReader.SKIP_DEBUG)

                        eventVisitor.getCurrentImplClassBytes()
                    }
                }

                implBytes?.let {
                    val implClassName = "${eventModule.className}Impl"
                    val implClassPath = "${implClassName.replace('.', '/')}.class"

                    jarOutput.putNextEntry(JarEntry(implClassPath))
                    jarOutput.write(it)
                    jarOutput.closeEntry()
                }
            }
        }

        jarFile.close()
        jarOutput.close()
    }


    private fun acceptProviderClassCollector(addOperateList: CopyOnWriteArrayList<ProviderInfo>, addEventList: CopyOnWriteArrayList<EventJarInfo>, file: File): ClassVisitor {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        return object : ProviderClassCollector(writer) {
            override fun methodExit(
                className: String?,
                interfaces: Array<String>
            ) {
                className?.let { implName ->
                    val interfaceName = interfaces.first()
                    addOperateList.add(ProviderInfo(interfaceName, implName, 0))
                }
            }

            override fun methodEdit(className: String?, targetName: String) {
                className?.let {
                    addEventList.add(EventJarInfo(className, targetName, file))
                }
            }
        }
    }


    private fun String.isModuleClass(): Boolean {
        return this.endsWith("Impl")
    }

    private fun parseProviders(providerInfos: CopyOnWriteArrayList<ProviderInfo>): List<ProviderInfo> {
        return providerInfos
            .sortedBy {
                it.implType
            }.mapIndexed { index, info ->
                info.copy(beanId = index + 1)
            }
    }


}