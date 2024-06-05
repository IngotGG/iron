package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

/**
 * Transforms a result set into a model representation, allowing for the model to be easily built. You shouldn't
 * call this method directly, instead use the mapper variant of the methods in [Iron] to automatically map the
 * result set to a model.
 * @author Santio
 * @since 1.0
 */
object ResultTransformer {
    private val arrayTransformations: Map<KClass<*>, (arr: Array<*>) -> Any?> = mapOf(
        List::class to { it.toList() },
        Set::class to { it.toSet() }
    )

    private fun <T: Any> read(result: ResultSet, clazz: KClass<T>): T {
        val entity = ModelTransformer.transform(clazz)

        val emptyConstructor = clazz.constructors.firstOrNull { it.parameters.isEmpty() }
        val fullConstructor = clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }

        if (emptyConstructor != null) {
            emptyConstructor.isAccessible = true
            val model = emptyConstructor.call()

            for (field in entity.fields) {
                val value = ValueTransformer.convert(result, field)
                if (value == null && !field.nullable) {
                    throw IllegalStateException("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                field.javaField.setAccessible(true)
                field.javaField.set(model, value)
            }

            return model
        } else if (fullConstructor != null) {
            fullConstructor.isAccessible = true

            val fields = entity.fields.map { field ->
                val value = ValueTransformer.convert(result, field)
                if (value == null && !field.nullable) {
                    throw IllegalStateException("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                value
            }

            return fullConstructor.call(*fields.toTypedArray())
        } else {
            throw IllegalStateException("No empty or full constructor found for model: $clazz")
        }
    }

    /**
     * Get the model from the result set at its current row.
     * @param clazz The class to map the result set to.
     * @return The model from the result set
     */
    fun <T: Any> ResultSet.model(clazz: KClass<T>): T {
        return read(this, clazz)
    }

    /**
     * Get the next model from the result set, if there is no next model then this method will return null.
     * @param clazz The class to map the result set to.
     * @return The model from the result set or null if there is no next model.
     */
    fun <T: Any> ResultSet.nextModel(clazz: KClass<T>): T? {
        return if (next()) {
            read(this, clazz)
        } else {
            null
        }
    }

    /**
     * Get all the models from the result set.
     * @param clazz The class to map the result set to.
     * @return A list of the models from the result set.
     * @since 1.0
     */
    fun <T: Any> ResultSet.models(clazz: KClass<T>): List<T> {
        val models = mutableListOf<T>()
        while (next()) {
            models.add(read(this, clazz))
        }
        return models
    }

    /**
     * Get the next model from the result set, if there is no next model then this method will return null.
     * @return The model from the result set or null if there is no next model.
     * @since 1.0
     */
    inline fun <reified T: Any> ResultSet.nextModel(): T? {
        return nextModel(T::class)
    }

    /**
     * Get all the models from the result set.
     * @return A list of the models from the result set.
     * @since 1.0
     */
    inline fun <reified T: Any> ResultSet.models(): List<T> {
        return models(T::class)
    }

}