package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.serialization.ColumnDeserializer
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Handles the conversion of any value to and from [ResultSet]s / prepared statements.
 * @author santio
 * @since 2.0
 */
internal class ValueTransformer(private val iron: Iron) {

    private fun getValue(resultSet: ResultSet, field: EntityField): Any? {
        return if (field.isArray || field.isCollection) {
            try {
                resultSet.getArray(field.convertedName(iron.settings.namingStrategy))
            } catch (ex: SQLException) {
                IllegalStateException("Failed to retrieve array value for field '${field.field.name}'", ex).printStackTrace()
                throw ex
            }
        } else {
            try {
                resultSet.getObject(field.convertedName(iron.settings.namingStrategy))
            } catch (ex: SQLException) {
                IllegalStateException("Failed to retrieve array value for field '${field.field.name}'", ex).printStackTrace()
                throw ex
            }
        }
    }

    /**
     * Takes a value from a [ResultSet] and converts it's appropriate java type.
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    fun fromResultSet(resultSet: ResultSet, field: EntityField, wrapOptional: Boolean = true): Any? {
        val value = this.getValue(resultSet, field)

        // Handle null values, if the field is nullable, we can just return null
        if (!field.nullable && value == null) {
            if (field.isOptional) {
                return Optional.empty<Any>()
            }

            error("Field '${field.field.name}' is not nullable but we received a null value.")
        } else if (value == null) {
            return null
        }

        // Handle optional values, we'll snatch the value from the result set then wrap it in an optional
        if (field.isOptional && wrapOptional) {
            return Optional.ofNullable(fromResultSet(resultSet, field, false))
        }

        // Check if the user provides us a deserializer
        val deserializer = field.deserializer as? ColumnDeserializer<Any, *>

        if (deserializer != null) {
            return deserializer.fromDatabaseValue(value)
        }

        // Handle arrays
        if (field.isArray && value is Array<*> && field.isEnum) {
            return value.map { iron.settings.enumTransformation.serialize(it as Enum<*>) }
        } else if (field.isArray && value is Array<*>) {
            return value
        } else if (field.isArray && value is Collection<*>) {
            return value.toTypedArray()
        } else if (field.isArray) {
            error("Field '${field.field.name}' is an array the database gave back ${value::class.java.name}")
        }

        // Handle collections
        if (field.isCollection && value is Collection<*> && field.isEnum) {
            return value.map { iron.settings.enumTransformation.serialize(it as Enum<*>) }
        } else if (field.isCollection && value is Collection<*>) {
            return value
        } else if (field.isCollection && value is Array<*>) {
            return value.toList()
        } else if (field.isCollection) {
            error("Field '${field.field.name}' is a collection but the database gave back ${value::class.java.name}")
        }

        // Handle enums based on the provided transformation
        if (field.isEnum && value is String) {
            return iron.settings.enumTransformation.deserialize(value, field.field.type)
        } else if (field.isEnum && value is Int) {
            return iron.settings.enumTransformation.deserialize(value.toString(), field.field.type)
        } else if (field.isEnum) {
            error("Field '${field.field.name}' is an enum but the database gave back ${value::class.java.name}")
        }

        // Handle booleans
        if (field.isBoolean && value is Boolean) {
            return value
        } else if (field.isBoolean && value is Int) {
            return value > 0
        } else if (field.isBoolean && value is String && !iron.settings.strictBooleans) {
            return value.equals("true", true) || value.equals("1") || value.equals("yes", true)
        } else if (field.isBoolean) {
            error("Field '${field.field.name}' is a boolean but the database gave back ${value::class.java.name}")
        }

        // Handle json
        if (field.isJson && value is String && iron.settings.serialization != null) {
            return iron.settings.serialization!!.deserialize(value, field.field.type)
        } else if (field.isJson) {
            error("Field '${field.field.name}' is json but the database gave back ${value::class.java.name}")
        }

        return value
    }

}