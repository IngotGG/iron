package gg.ingot.iron.model

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Column
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinProperty

/**
 * Represents a field in a class along with its information
 * @author santio
 * @since 2.0
 */
data class ModelField(
    val iron: Iron,
    val memberProperty: KProperty<*>?,
    val java: Field,
    val kotlin: KProperty<*>? = java.kotlinProperty,
) {

    init {
        val accessible = java.trySetAccessible()
        if (!accessible) error("Failed to set field '${java.name}' accessible")
    }

    inline fun <reified T: Annotation> annotation(): T? {
        val klass = java.declaringClass.kotlin
        return if (memberProperty != null) {
            memberProperty.findAnnotation<T>()
        } else {
            java.annotations.find { it is T } as T?
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val details by lazy { annotation<Column>() }

    /**
     * @return The name of the column in the database
     */
    val column get() = details?.name?.takeIf { it.isNotBlank() }
        ?: iron.inflector.columnName(java)

    /**
     * @return The name to use when referencing the field in a named parameter
     */
    val variable get() = details?.variable?.takeIf { it.isNotBlank() }
        ?: java.name

    /**
     * @return Whether the field is ignored or not completely by Iron
     */
    fun isIgnored(): Boolean {
        return Modifier.isTransient(java.modifiers)
            || details?.ignore == true
            || java.isSynthetic
    }

    /**
     * @return Whether the field is a primary key in the table or not
     */
    fun isPrimaryKey(): Boolean {
        return details?.primaryKey == true
    }

}