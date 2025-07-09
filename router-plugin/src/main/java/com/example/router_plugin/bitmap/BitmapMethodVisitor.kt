package com.example.router_plugin.bitmap

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class BitmapMethodVisitor(
    mv: MethodVisitor,
    access: Int,
    name: String?,
    desc: String?,
) : AdviceAdapter(Opcodes.ASM9, mv, access, name, desc) {

    override fun visitMethodInsn(
        opcodeAndSource: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        val isBitmapCreation = opcodeAndSource == Opcodes.INVOKESTATIC &&
                (owner == "android/graphics/Bitmap" || owner == "android/graphics/BitmapFactory") &&
                (name?.startsWith("create") == true || name?.startsWith("decode") == true)

        if (isBitmapCreation) {
            onBitmapCreate(owner, name)
        }

        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
    }

    private fun onBitmapCreate(owner: String?, name: String?) {
        // 在栈上复制返回的 Bitmap 对象
        dup()

        // 获取当前线程堆栈
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/Thread",
            "currentThread",
            "()Ljava/lang/Thread;",
            false
        )

        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/Thread",
            "getStackTrace",
            "()[Ljava/lang/StackTraceElement;",
            false
        )

        // 调用 BitmapMonitor.track 方法
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "com/example/monitor/BitmapMonitor",
            "track",
            "(Landroid/graphics/Bitmap;[Ljava/lang/StackTraceElement;)V",
            false
        )
    }
}