package gg.ingot.iron.transformer

import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.serialization.*
import gg.ingot.iron.strategies.NamingStrategy
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Transforms a value from a [ResultSet] into its corresponding type.
 * @author DebitCardz
 * @since 1.2
 */
internal class ValueTransformer(
    private val serializationAdapter: SerializationAdapter?
) {
    private val arrayTransformations: Map<KClass<*>, (Array<*>) -> Collection<*>> = mapOf(
        List::class to { it.toList() },
        Set::class to { it.toSet() }
    )

    /**
     * Retrieve the value from the result set for the given field.
     * Will automatically convert an [Array] into a given [Collection] type if the field is said [Collection].
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    fun convert(
        resultSet: ResultSet,
        field: EntityField,
        namingStrategy: NamingStrategy
    ): Any? {
        return if(field.deserializer != null) toCustomDeserializedObj(resultSet, field, namingStrategy)
        else if(field.isJson) toJsonObject(resultSet, field, namingStrategy)
        else if(field.isArray) toArray(resultSet, field, namingStrategy)
        else if(field.isCollection) toCollection(resultSet, field, namingStrategy)
        else toObject(resultSet, field, namingStrategy)
    }

    /**
     * Retrieve the value as an [Array] from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param columnName The column name to retrieve the value for.
     */
    private fun toArray(resultSet: ResultSet, field: EntityField, namingStrategy: NamingStrategy): Any? {
        val arr = resultSet.getArray(field.convertedName(namingStrategy))
            ?.array

        if(
            arr is Array<*>
            && field.isEnum
            && field.isArray
        ) {
            // map to enum values
            return arr.map {
                java.lang.Enum.valueOf(field.javaField.type.componentType as Class<out Enum<*>>, it as String)
            }.toTypedArray()
        }

        return arr
    }

    /**
     * Retrieve the value as a [Collection] from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param columnName The column name to retrieve the value for.
     * @param type The type of the collection.
     */
    private fun toCollection(resultSet: ResultSet, field: EntityField, namingStrategy: NamingStrategy): Collection<*> {
        val arr = toArray(resultSet, field, namingStrategy) as Array<*>

        val transformation = arrayTransformations.entries
            // retrieve the first transformation that matches the type
            .firstOrNull { it.key.java.isAssignableFrom(field.javaField.type) }
            ?.value
            ?: error("Unsupported collection type: ${field.javaField.type.name}")

        return transformation(arr)
    }

    /**
     * Retrieve the value as an object from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param columnName The column name to retrieve the value for.
     * @return The value from the result set.
     */
    private fun toObject(resultSet: ResultSet, field: EntityField, namingStrategy: NamingStrategy): Any? {
        val value = resultSet.getObject(field.convertedName(namingStrategy))

        // Automatically map the enum to the enum type
        if(field.isEnum && field.deserializer == null) {
            return java.lang.Enum.valueOf(field.javaField.type as Class<out Enum<*>>, value as String)
        }

        // Automatically convert Ints to Booleans for DBMS
        // that don't give us back a boolean type.
        if(value != null && value is Int && field.isBoolean) {
            if(value != 0 && value != 1) {
                error("Expected a boolean value, but found an integer value of $value for field: ${field.field.name}")
            }
            return value == 1
        }

        return value
    }

    /**
     * Retrieve the value as a deserialized JSON object from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    private fun toJsonObject(resultSet: ResultSet, field: EntityField, namingStrategy: NamingStrategy): Any? {
        checkNotNull(serializationAdapter) {
            "A serializer adapter has not been passed through IronSettings, you will not be able to automatically deserialize JSON."
        }

        val obj = toObject(resultSet, field, namingStrategy)
            ?: return null

        //todo: support binary
        return serializationAdapter.deserialize(obj.toString(), field.javaField.type)
    }

    /**
     * Retrieve the value as a deserialized object from the provided
     * [ColumnDeserializer].
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    private fun toCustomDeserializedObj(resultSet: ResultSet, field: EntityField, namingStrategy: NamingStrategy): Any? {
        val obj = toObject(resultSet, field, namingStrategy)
            ?: return null

        val deserializer = field.deserializer as? ColumnDeserializer<Any, *>
            ?: error("Deserializer is null, but it should not be.")

        return deserializer.fromDatabaseValue(obj)
    }
}