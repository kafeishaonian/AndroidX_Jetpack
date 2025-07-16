package com.example.router.log

object ELog {


    @JvmStatic
    fun <T> log(clazz: Class<T>): T {
        val implClassName = clazz.name + "Impl"
        return runCatching {
            val implClass = Class.forName(implClassName)
            val instance = implClass.getDeclaredConstructor().newInstance() as T
            instance
        }.getOrElse { e->
            throw RuntimeException("ELog 无法创建 $implClassName，请检查 ASM 插桩是否生效", e)
        }
    }


}