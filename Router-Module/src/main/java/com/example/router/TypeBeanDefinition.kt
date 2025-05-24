package com.example.router

class TypeBeanDefinition<T>(
    val targetClass: Class<*>,
    beanId: Int,
    sourceClass: Class<T>
) : BeanDefinition<T>(
    beanId = beanId,
    sourceClass = sourceClass
)