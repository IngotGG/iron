package gg.ingot.iron.representation

import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * A representation of a class that can be exploded into an array of its fields. This is useful for
 * building SQL queries and other operations that require the fields of a class to be exploded.
 * @author DebitCardz
 * @since 1.3
 */
interface ExplodingModel {
    /**
     * Explodes the model into an array of its fields.
     * @return An array of the fields of the model.
     */
    fun explode(): Array<Any> {
        val kClass = this::class

        val fields = cachedFields.getOrPut(kClass) {
            kClass.declaredMemberProperties
                .filterNot { it.javaField?.isSynthetic == true }
                .onEach { it.isAccessible = true }
                .map { it.javaField ?: error("${it.name} has no backing field.") }
        }

        return Array(fields.size) { fields[it].get(this) }
    }

    companion object {
        /** A cache of fields for each model. */
        internal val cachedFields = HashMap<KClass<*>, List<Field>>()
    }
}