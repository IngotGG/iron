package gg.ingot.iron.transformer

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

internal fun isArray(field: KProperty<*>): Boolean {
    return field.javaField?.type?.isArray ?: false
}

internal fun isArray(kClass: KClass<*>): Boolean {
    return kClass.java.isArray
}

internal fun isCollection(field: KProperty<*>): Boolean {
    return field.javaField?.type?.let { Collection::class.java.isAssignableFrom(it) } ?: false
}

internal fun isCollection(kClass: KClass<*>): Boolean {
    return Collection::class.java.isAssignableFrom(kClass.java)
}

internal fun isEnum(field: KProperty<*>): Boolean {
    val javaField = field.javaField ?: return false

    return javaField.type.isEnum
            || javaField.type.isArray
            && javaField.type.componentType.isEnum
}

internal fun isEnum(kClass: KClass<*>): Boolean {
    return kClass.java.isEnum
            || kClass.java.isArray
            && kClass.java.componentType.isEnum
}