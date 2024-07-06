package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.strategies.NamingStrategy
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Transforms a result set into a model representation, allowing for the model to be easily built. You shouldn't
 * call this method directly, instead use the mapper variant of the methods in [Iron] to automatically map the
 * result set to a model.
 * @author Santio
 * @since 1.0
 */
internal class ResultTransformer(
    private val modelTransformer: ModelTransformer,
    private val valueTransformer: ValueTransformer
) {
    /**
     * Reads the result set and transforms it into a model or value.
     * @param result The result set to read from.
     * @param clazz The class to transform the result to.
     * @param columnLabel The column label to read from a value.
     */
    fun <T: Any> read(
        result: ResultSet,
        clazz: KClass<T>,
        columnLabel: String? = null
    ): T? {
        return if(clazz.annotations.any { it is Model }) readModel(result, clazz) else readValue(result, clazz, columnLabel)
    }

    /**
     * Reads the result set and transforms it into a model.
     * @param result The result set to read from.
     * @param clazz The class to transform the result to.
     * @return The model from the result set.
     */
    private fun <T : Any> readModel(result: ResultSet, clazz: KClass<T>): T {
        val entity = modelTransformer.transform(clazz)

        val emptyConstructor = clazz.constructors.firstOrNull { it.parameters.isEmpty() }
        // prefer primary constructor first
        // then try to use secondary constructors if they have the same length of fields.
        val fullConstructor = clazz.primaryConstructor
            ?.takeIf { it.parameters.size == entity.fields.size }
            ?: clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }

        if (emptyConstructor != null) {
            emptyConstructor.isAccessible = true
            val model = emptyConstructor.call()

            for (field in entity.fields) {
                val value = valueTransformer.convert(result, field, entity.namingStrategy)
                if (value == null && !field.nullable) {
                    error("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                field.javaField.setAccessible(true)
                field.javaField.set(model, value)
            }

            return model
        } else if (fullConstructor != null) {
            fullConstructor.isAccessible = true

            val fields = entity.fields.map { field ->
                val value = valueTransformer.convert(result, field, entity.namingStrategy)
                if (value == null && !field.nullable) {
                    error("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                value
            }

            return fullConstructor.call(*fields.toTypedArray())
        } else {
            error("No empty or full constructor found for model: $clazz")
        }
    }

    /**
     * Reads the result set and transforms it into a value.
     * @param result The result set to read from.
     * @param clazz The class to transform the result to.
     * @param columnLabel The column label to read from a value.
     * @return The value from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> readValue(
        result: ResultSet,
        clazz: KClass<T>,
        columnLabel: String? = null
    ): T? {
        val javaClass = clazz.java
        if(Collection::class.java.isAssignableFrom(javaClass))
            error("Use an Array instead of a Collection")

        if(javaClass.isArray) {
            if(columnLabel != null) result.getArray(columnLabel)?.array as? T
            else result.getArray(1)?.array as? T
        }

        if(javaClass.isEnum) {
            val value = if(columnLabel != null) result.getString(columnLabel) else result.getString(1)
            return java.lang.Enum.valueOf(javaClass as Class<out Enum<*>>, value) as? T
        }

        try {
            return if(columnLabel != null) result.getObject(columnLabel) as? T
            else result.getObject(1) as? T
        } catch(ex: ClassCastException) {
            error("Could not cast the column to the desired type, if you're attempted to map to a model try annotating with @Model, if not try passing a custom deserializer.")
        }
    }
}