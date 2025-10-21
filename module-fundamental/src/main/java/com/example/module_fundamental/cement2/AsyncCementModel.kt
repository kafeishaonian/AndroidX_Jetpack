package com.example.module_fundamental.cement2

import kotlin.reflect.KClass

/**
 * 异步Cement Model
 * 用于异步构建的Model，要求状态类型为data class或基本类型
 * 
 * @param M 状态类型（必须是data class、基本类型、Unit或String）
 * @param VH ViewHolder类型
 */
abstract class AsyncCementModel<M : Any, VH : CementViewHolder>(
    val state: M
) : CementModel<VH>() {
    
    init {
        // 检查state类型是否合法
        checkType(state::class)
    }
    
    /**
     * 判断内容是否相同
     * 通过比较state来判断
     */
    override fun isContentTheSame(item: CementModel<*>): Boolean {
        return item is AsyncCementModel<*, *> && item.state == this.state
    }
}

/**
 * 检查类型是否为data class或基本类型
 */
internal fun <T : Any> checkType(clazz: KClass<T>) {
    when (clazz) {
        Boolean::class,
        Char::class,
        Byte::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        Unit::class,
        String::class -> {
            // 基本类型和String是允许的
            return
        }
        else -> {
            // 检查是否为data class
            if (isData(clazz.java)) {
                return
            }
            throw IllegalStateException(
                "$clazz should be Primitive or Unit or String or `data class`"
            )
        }
    }
}

/**
 * 检查类是否为data class
 */
private fun isData(clazz: Class<*>): Boolean {
    // 通过检查是否存在data class特有的方法来判断
    return try {
        val methods = clazz.declaredMethods
        val hasCopy = methods.any { it.name == "copy\$default" }
        val hasComponent1 = methods.any { it.name == "component1" }
        val hasEquals = methods.any { it.name == "equals" }
        val hasHashCode = methods.any { it.name == "hashCode" }
        
        hasCopy && hasComponent1 && hasEquals && hasHashCode
    } catch (e: SecurityException) {
        false
    }
}