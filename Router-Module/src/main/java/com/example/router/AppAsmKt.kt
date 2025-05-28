package com.example.router


@Synchronized
fun <T> AppAsmContext.getBean(definition: BeanDefinition<T>): T =
    definition.sourceClass.getDeclaredConstructor().newInstance()


fun <T> AppAsmContext.getBean(clazz: Class<T>): T {
    val typeBeanDefinition = beanDefinitionTypeMap[clazz]

    if (typeBeanDefinition is TypeBeanDefinition<*>) {
        return getBean(typeBeanDefinition) as T
    }

    return throw IllegalStateException("未找到 ${clazz.name} 的Bean定义")
}