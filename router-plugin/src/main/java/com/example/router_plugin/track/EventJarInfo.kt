package com.example.router_plugin.track

import java.io.File

data class EventJarInfo(
    val className: String,
    val tableName: String,
    val jarFile: File
)