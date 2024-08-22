package gg.ingot.iron.controller.controller

fun interface Interceptor<T> {
    fun intercept(entity: T): T
}