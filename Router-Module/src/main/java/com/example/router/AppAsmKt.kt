package com.example.router


@Synchronized
fun <T : Any> AppAsmContext.getBean(definition: BeanDefinition<T>): T =
    definition.sourceClass.getDeclaredConstructor().newInstance()


fun <T : Any> AppAsmContext.getBean(clazz: Class<T>): T {
    val typeBeanDefinition = beanDefinitionTypeMap[clazz]?.takeIf { definition ->
        clazz.isAssignableFrom(definition::class.java) &&
                definition is TypeBeanDefinition<*>
    } as? TypeBeanDefinition<T>

    return typeBeanDefinition?.let {
        getBean(it)
    } ?: throw IllegalStateException("未找到 ${clazz.name} 的Bean定义")
}