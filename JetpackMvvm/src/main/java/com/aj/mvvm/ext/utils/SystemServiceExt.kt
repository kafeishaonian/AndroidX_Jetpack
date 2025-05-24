package com.aj.mvvm.ext.utils

import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat


inline fun <reified T> Context.getSystemService(): T? =
    ContextCompat.getSystemService(this, T::class.java)

val Context.clipboardManager get() = getSystemService<ClipboardManager>()