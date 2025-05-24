package com.example.router

abstract class BeanDefinition<T>(
    val beanId: Int,
    val sourceClass: Class<T>
)