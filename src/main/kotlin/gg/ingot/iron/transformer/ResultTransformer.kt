package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import java.sql.ResultSet
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

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
        clazz: Class<T>,
        columnLabel: String? = null
    ): T? {
        return if (clazz.annotations.any { it is Model }) readModel(result, clazz) else readValue(
            result,
            clazz,
            columnLabel
        )
    }

    /**
     * Reads the result set and transforms it into a model.
     * @param result The result set to read from.
     * @param clazz The class to transform the result to.
     * @return The model from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> readModel(result: ResultSet, clazz: Class<T>): T {
        val entity = modelTransformer.transform(clazz)
        val emptyConstructor = clazz.constructors.firstOrNull { it.parameters.isEmpty() }

        val fullConstructor = if (clazz.isRecord) {
            clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }
        } else if (clazz.kotlin.primaryConstructor != null) {
            clazz.kotlin.primaryConstructor?.javaConstructor
        } else {
            clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }
        }

        if (emptyConstructor != null) {
            emptyConstructor.isAccessible = true
            val model: T = emptyConstructor.newInstance() as T

            for (field in entity.fields) {
                val value = valueTransformer.convert(result, field, entity.namingStrategy)

                if (value == null && !field.nullable) {
                    error("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                field.field.setAccessible(true)
                field.field.set(model, value)
            }

            return model
        } else if (fullConstructor != null) {
            fullConstructor.isAccessible = true

            val arguments = entity.fields.map { field ->
                val value = valueTransformer.convert(result, field, entity.namingStrategy)

                if (value == null && !field.nullable) {
                    error("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                value
            }.toMutableList()

            if (fullConstructor.parameters.last().type == DefaultConstructorMarker::class.java) {
                arguments.add(null)
            }

            try {
                return fullConstructor.newInstance(*arguments.toTypedArray()) as T
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
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
        clazz: Class<T>,
        columnLabel: String? = null
    ): T? {
        if(Collection::class.java.isAssignableFrom(clazz))
            error("Use an Array instead of a Collection")

        if(clazz.isArray) {
            if(columnLabel != null) result.getArray(columnLabel)?.array as? T
            else result.getArray(1)?.array as? T
        }

        if(clazz.isEnum) {
            val value = if(columnLabel != null) result.getString(columnLabel) else result.getString(1)
            return java.lang.Enum.valueOf(clazz as Class<out Enum<*>>, value) as? T
        }

        try {
            val obj = if(columnLabel != null) result.getObject(columnLabel) as? T
            else result.getObject(1) as? T

            // Automatically convert Ints to Booleans for DBMS
            // that don't give us back a boolean type.
            @Suppress("KotlinConstantConditions")
            if(obj != null && box(clazz) == Boolean::class.java && obj is Int) {
                if(obj != 0 && obj != 1) {
                    error("Could not convert the column to a boolean, the value was not 0 or 1.")
                }

                return (obj == 1) as? T
            }

            return obj
        } catch(ex: ClassCastException) {
            error("Could not cast the column to the desired type, if you're attempted to map to a model try annotating with @Model, if not try passing a custom deserializer.")
        }
    }

    /**
     * Maps a primitive type to its java boxed counterpart.
     * @param type The type to map.
     * @return The boxed type.
     */
    private fun box(type: Class<*>): Class<*> {
        return when (type) {
            Class.forName("java.lang.Boolean") -> Boolean::class.java
            Boolean::class.javaPrimitiveType -> Boolean::class.java
            Byte::class.javaPrimitiveType -> Byte::class.java
            Char::class.javaPrimitiveType -> Char::class.java
            Short::class.javaPrimitiveType -> Short::class.java
            Int::class.javaPrimitiveType -> Int::class.java
            Long::class.javaPrimitiveType -> Long::class.java
            Float::class.javaPrimitiveType -> Float::class.java
            Double::class.javaPrimitiveType -> Double::class.java
            Void::class.javaPrimitiveType -> Void::class.java
            else -> type
        }
    }
}