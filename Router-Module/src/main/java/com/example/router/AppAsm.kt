package com.example.router

import kotlin.reflect.KClass

object AppAsm {

    private val appAsmContext by lazy {
        AppAsmContext()
    }

    private fun <T> getBean(cls: Class<T>): T {
        return appAsmContext.getBean(cls)
    }


    @JvmStatic
    fun <T> getRouter(clazz: Class<T>): T = getBean(clazz)


    fun <T : Any> getRouter(kClazz: KClass<T>) : T = getBean(kClazz.java)

}