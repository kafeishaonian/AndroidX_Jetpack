package com.example.router.bitmap

import android.graphics.Bitmap

data class Meta(
    val width: Int,
    val height: Int,
    val config: Bitmap.Config?,
    val byteCount: Int,
    val allocationTime: Long,
    val stackTrace: String
) {
    val sizeDescription: String
        get() = "${width}x${height} ${config?.name ?: "UNKNOWN"} (${byteCount / 1024}KB)"
}
