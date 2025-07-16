package com.example.router.log

data class EventData (
    val moduleName: String,
    val type: String,
    val eventId: String,
    val page: String,
    val action: String,
    val params: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)