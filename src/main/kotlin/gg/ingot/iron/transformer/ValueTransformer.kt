package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.transformer.adapter.*
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Handles the conversion of any value to and from [ResultSet]s / prepared statements.
 * @author santio
 * @since 2.0
 */
internal class ValueTransformer(private val iron: Iron) {

    /**
     * Takes a value from a [ResultSet] and converts it's appropriate java type.
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    fun fromResultSet(resultSet: ResultSet, field: EntityField): Any? {
        val value = if (field.isArray || field.isCollection) {
            try {
                resultSet.getArray(field.convertedName(iron.settings.namingStrategy)).array
            } catch (ex: SQLException) {
                throw IllegalStateException("Failed to retrieve array value for field '${field.name}'", ex)
            }
        } else {
            try {
                resultSet.getObject(field.convertedName(iron.settings.namingStrategy))
            } catch (ex: SQLException) {
                throw IllegalStateException("Failed to retrieve value for field '${field.name}'", ex)
            }
        }

        return deserialize(value, field)
    }

    /**
     * Takes a value from a [ResultSet] and converts it to it's appropriate java type, or if it's a
     * model, we'll send it to the model transformer to convert it.
     * @param resultSet The result set to retrieve the value from.
     * @param label The label to retrieve the value for.
     * @param clazz The class to retrieve the value for.
     * @return The value from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    fun read(resultSet: ResultSet, label: String?, clazz: Class<*>): Any? {
        if (clazz.annotations.any { it is Model }) {
            return iron.modelTransformer.readModel(resultSet, clazz)
        }

        val value = if (label == null)
            resultSet.getObject(1)
        else if (clazz.isArray) resultSet.getArray(label).array
        else resultSet.getObject(label)

        if (value == null) return null

        return if (clazz.isArray) {
            value as Array<*>
        } else if (clazz.isEnum) {
            java.lang.Enum.valueOf(clazz as Class<out Enum<*>>, value as String)
        } else if (Collection::class.java.isAssignableFrom(clazz)) {
            (value as Array<*>).toList()
        } else if (Set::class.java.isAssignableFrom(clazz)) {
            (value as Array<*>).toSet()
        } else if (box(clazz) == Boolean::class.java) {
            when (value) {
                true -> true
                is Int -> {
                    when (value) {
                        0 -> false
                        1 -> true
                        else -> error("Failed to convert boolean value for field '${label}'")
                    }
                }
                else -> {
                    error("Failed to convert boolean value for field '${label}'")
                }
            }
        } else {
            value
        }
    }

    private fun getValueAdapter(field: EntityField): ValueAdapter<*>? {
        return if (field.isArray) {
            ArrayValueAdapter
        } else if (field.isSet) {
            SetValueAdapter
        } else if (field.isCollection) {
            CollectionValueAdapter
        } else if (field.isJson) {
            JsonValueAdapter
        } else if (box(field.field.java.type) == Boolean::class.java) {
            BooleanValueAdapter
        } else if (field.getUnderlyingType().isEnum) {
            EnumValueAdapter
        } else {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun deserialize(value: Any?, field: EntityField, wrapOptional: Boolean = true): Any? {
        // Handle null values
        if (value == null && !field.nullable && !field.isOptional) {
            error("Field '${field.name}' is not nullable but we received a null value.")
        } else if (value == null && field.isOptional) {
            return Optional.empty<Any>()
        } else if (value == null) {
            return null
        }

        // Handle transformations
        return if (field.isOptional && wrapOptional) {
            Optional.ofNullable(deserialize(value, field, false))
        } else if (field.deserializer != null) {
            // User provided deserializer
            val deserializer = field.deserializer as ColumnDeserializer<Any, *>
            deserializer.fromDatabaseValue(value)
        } else {
            val adapter = getValueAdapter(field)
                ?: return value

            adapter.fromDatabaseValue(value, iron, field)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun serialize(value: Any?, field: EntityField): Any? {
        // Unwrap optionals
        if (value is Optional<*>) {
            return serialize(value.orElse(null), field)
        }

        // Handle null values
        if (value == null && !field.nullable) {
            error("Field '${field.name}' is not nullable but we received a null value.")
        } else if (value == null) {
            return null
        }

        return if (field.serializer != null) {
            // User provided serializer
            val serializer = field.serializer as ColumnSerializer<Any, *>
            serializer.toDatabaseValue(value)
        } else {
            val adapter = getValueAdapter(field) as ValueAdapter<Any>?
                ?: return value

            adapter.toDatabaseValue(value, iron, field)
        }
    }
}

/**
 * Maps a primitive type to its java boxed counterpart.
 * @param type The type to map.
 * @return The boxed type.
 */
internal fun box(type: Class<*>): Class<*> {
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