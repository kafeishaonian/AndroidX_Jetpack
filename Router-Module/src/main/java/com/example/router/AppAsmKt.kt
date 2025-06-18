package com.example.router


@Synchronized
fun <T> AppAsmContext.getBean(definition: BeanDefinition<T>): T {

    val sourceClass = definition.sourceClass
    var instance = beanDefinitionInstanceMap[sourceClass]

    if (instance == null)  {
        val newInstance = sourceClass.getDeclaredConstructor().newInstance()
        beanDefinitionInstanceMap[sourceClass] = newInstance as Any
        instance = newInstance
    }
    return instance as T
}


fun <T> AppAsmContext.getBean(clazz: Class<T>): T {
    val typeBeanDefinition = beanDefinitionTypeMap[clazz]

    if (typeBeanDefinition is TypeBeanDefinition<*>) {
        return getBean(typeBeanDefinition) as T
    }

    return throw IllegalStateException("未找到 ${clazz.name} 的Bean定义")
}