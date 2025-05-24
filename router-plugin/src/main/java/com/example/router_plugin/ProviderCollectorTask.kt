package com.example.router_plugin

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
import org.gradle.internal.impldep.bsh.commands.dir
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

internal abstract class ProviderCollectorTask : DefaultTask() {

//    @get:Incremental
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
    fun taskAction(inputChanges: InputChanges) {
        val addOperateList = mutableListOf<String>()

        var needOparate: ByteArray? = null

        println("---开始执行代码---->")
        val outputFile = output.get().asFile
        println("输出文件路径: ${outputFile.absolutePath}")

        val fileOutputStream = FileOutputStream(outputFile)
        println("FileOutputStream 已打开: ${outputFile.absolutePath}")
        JarOutputStream(fileOutputStream).use { jarOut ->
            println("---进入 use 块--11-")
            processTargetJars(jarOut, addOperateList) {
                needOparate = it
            }
            println("---进入 use 块--22-")
            processDirs(jarOut, addOperateList)
            println("---进入 use 块--33-")
        }
    }

    private fun processDirs(jarOutPut: JarOutputStream,  addOperateList: MutableList<String>) {
        allDirectories.get().onEach { dir ->
            dir.asFile.walk().forEach { file ->
                if (file.isFile) {
                    println("jar file:= ${file.name}")
                }
            }
        }
    }

    private fun processTargetJars(
        jarOutPut: JarOutputStream,
        addOperateList: MutableList<String>,
        needOperateByteArrayList: (it: ByteArray) -> Unit
    ) {
        allJars.get().forEach { file -> {
            println("---22222----> ${file.asFile.absolutePath}")
            JarFile(file.asFile).use { jarFile->
                println("---33333----> ${jarFile.name}")
                jarFile.entries().iterator().forEach { jarEntry ->
                    println("---44444----> ${jarEntry.name}")
//                    if (!jarEntry.isDirectory && jarEntry.name.contains("com.example.router.AppAsmContext")) {
//                        println("------> name:= ${jarEntry.name}")
//                        jarFile.getInputStream(jarEntry).use {
//                            needOperateByteArrayList.invoke(it.readAllBytes())
//                        }
//                    } else {
//                        runCatching {
//                            println("=======> name:= ${jarEntry.name}")
//
//                            if (isTargetProxyClass(jarEntry.name ?: "")) {
//                                addOperateList.add(jarEntry.name)
//                            }
//
//                            jarOutPut.putNextEntry(JarEntry(jarEntry.name))
//
//                            jarFile.getInputStream(jarEntry).use {
//                                it.copyTo(jarOutPut)
//                            }
//                        }
//                        jarOutPut.closeEntry()
//                    }
                }
            }
        } }
    }

    private fun isTargetProxyClass(filePath: String): Boolean {
        if (filePath.startsWith("com.example.router") && filePath.endsWith("RouterProvider")) {
            return true
        }
        return false
    }
}