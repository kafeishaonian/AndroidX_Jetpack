package com.example.router

object AppAsm {

    private val appAsmContext by lazy {
        AppAsmContext()
    }

    private fun <T : Any> getBean(cls: Class<T>): T {
        return appAsmContext.getBean(cls)
    }


    @JvmStatic
    fun <T : Any> getRouter(clazz: Class<T>): T = getBean(clazz)

}