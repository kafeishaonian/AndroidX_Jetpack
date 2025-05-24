package com.example.router

class AppAsmContext {

    val beanDefinition = mutableListOf<BeanDefinition<*>>()
    val beanDefinitionTypeMap = mutableMapOf<Class<*>, TypeBeanDefinition<*>>()

    init {
        initBeanDefinition()
        fillBeanDefinitionMap()
    }


    private fun initBeanDefinition() {
    }


    private fun fillBeanDefinitionMap() {
        for (definition in beanDefinition) {
            if (definition is TypeBeanDefinition<*>) {
                beanDefinitionTypeMap[definition.targetClass] = definition
            }
        }
    }

}