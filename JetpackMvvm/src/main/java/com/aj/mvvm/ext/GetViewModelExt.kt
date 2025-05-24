package com.aj.mvvm.ext

import com.aj.mvvm.base.viewmodel.BaseViewModel
import java.lang.reflect.ParameterizedType

/**
 * 获取当前类绑定的泛型
 */
@Suppress("UNCHECKED_CAST")
fun <VM: BaseViewModel> getVmClazz(obj: Any): Class<VM> {
    return (obj.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VM>
}