package gg.ingot.iron.transformer

import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.SerializationAdapter
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
    fun convert(resultSet: ResultSet, field: EntityField): Any? {
        val type = field.javaField.type

        return when {
            field.deserializer != null -> toCustomDeserializedObj(resultSet, field)
            field.isJson -> toJsonObject(resultSet, field)
            type.isArray -> toArray(resultSet, field.columnName)
            Collection::class.java.isAssignableFrom(type) -> toCollection(resultSet, field.columnName, type)
            else -> return toObject(resultSet, field.columnName)
        }
    }

    /**
     * Retrieve the value as an [Array] from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param columnName The column name to retrieve the value for.
     */
    private fun toArray(resultSet: ResultSet, columnName: String): Any? {
        return resultSet.getArray(columnName)
            ?.array
    }

    /**
     * Retrieve the value as a [Collection] from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param columnName The column name to retrieve the value for.
     * @param type The type of the collection.
     */
    private fun toCollection(resultSet: ResultSet, columnName: String, type: Class<*>): Collection<*> {
        val arr = toArray(resultSet, columnName) as Array<*>

        val transformation = arrayTransformations.entries
            // retrieve the first transformation that matches the type
            .firstOrNull { it.key.java.isAssignableFrom(type) }
            ?.value
            ?: error("Unsupported collection type: $type")

        return transformation(arr)
    }

    /**
     * Retrieve the value as an object from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param columnName The column name to retrieve the value for.
     * @return The value from the result set.
     */
    private fun toObject(resultSet: ResultSet, columnName: String): Any? {
        return resultSet.getObject(columnName)
    }

    /**
     * Retrieve the value as a deserialized JSON object from the result set.
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    private fun toJsonObject(resultSet: ResultSet, field: EntityField): Any? {
        checkNotNull(serializationAdapter) {
            "A serializer adapter has not been passed through IronSettings, you will not be able to automatically deserialize JSON."
        }

        val obj = toObject(resultSet, field.columnName)
            ?: return null

        return serializationAdapter.deserialize(obj, field.javaField.type)
    }

    /**
     * Retrieve the value as a deserialized object from the provided
     * [ColumnDeserializer].
     * @param resultSet The result set to retrieve the value from.
     * @param field The field to retrieve the value for.
     * @return The value from the result set.
     */
    private fun toCustomDeserializedObj(resultSet: ResultSet, field: EntityField): Any? {
        val obj = toObject(resultSet, field.columnName)
            ?: return null

        val deserializer = field.deserializer as? ColumnDeserializer<Any, *>
            ?: error("Deserializer is null, but it should not be.")

        return deserializer.deserialize(obj)
    }
}