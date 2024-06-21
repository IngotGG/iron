package gg.ingot.iron.transformer

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

/**
 * Check to see whether the given field is an array.
 * @param field The field to check.
 * @return Whether the field is an array.
 */
internal fun isArray(field: KProperty<*>): Boolean {
    return field.javaField?.type?.isArray ?: false
}

/**
 * Check to see whether the given class is an array.
 * @param kClass The class to check.
 * @return Whether the class is an array
 */
internal fun isArray(kClass: KClass<*>): Boolean {
    return kClass.java.isArray
}

/**
 * Check to see whether the given field is a collection.
 * @param field The field to check.
 * @return Whether the field is a collection.
 */
internal fun isCollection(field: KProperty<*>): Boolean {
    return field.javaField?.type?.let { Collection::class.java.isAssignableFrom(it) } ?: false
}

/**
 * Check to see whether the given class is a collection.
 * @param kClass The class to check.
 * @return Whether the class is a collection.
 */
internal fun isCollection(kClass: KClass<*>): Boolean {
    return Collection::class.java.isAssignableFrom(kClass.java)
}

/**
 * Check to see whether the given field is an enum, a collection of enums
 * or an array of enums.
 * @param field The field to check.
 * @return Whether the field is an enum, a collection of enums or an array of enums.
 */
internal fun isEnum(field: KProperty<*>): Boolean {
    val javaField = field.javaField ?: return false

    return javaField.type.isEnum
            || javaField.type.isArray
            && javaField.type.componentType.isEnum
}

/**
 * Check to see whether the given class is an enum, a collection of enums
 * or an array of enums.
 * @param kClass The class to check.
 * @return Whether the class is an enum, a collection of enums or an array of enums.
 */
internal fun isEnum(kClass: KClass<*>): Boolean {
    return kClass.java.isEnum
            || kClass.java.isArray
            && kClass.java.componentType.isEnum
}