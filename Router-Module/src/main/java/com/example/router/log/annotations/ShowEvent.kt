package com.example.router.log.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ShowEvent(val eventId: String, val page: String, val action: String)
