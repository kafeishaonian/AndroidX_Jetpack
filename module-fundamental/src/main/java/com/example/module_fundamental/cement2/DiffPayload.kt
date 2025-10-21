package com.example.module_fundamental.cement2

/**
 * Diff载荷
 * 用于在DiffUtil更新时传递旧的Model信息
 */
class DiffPayload(val model: CementModel<*>) {
    
    companion object {
        /**
         * 从payloads列表中提取DiffPayload中的Model
         * @param payloads payload列表
         * @return 提取的Model，如果没有找到则返回null
         */
        fun extract(payloads: List<Any?>): CementModel<*>? {
            for (payload in payloads) {
                if (payload is DiffPayload) {
                    return payload.model
                }
            }
            return null
        }
    }
}