package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.params.ColumnJsonField
import gg.ingot.iron.sql.params.ColumnSerializedField
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Transforms a placeholder value into a value that can be used in a SQL query. This is useful for
 * transforming serialized fields and JSON fields into a format that can be used in a query.
 * @since 1.3
 * @author DebitCardz
 */
internal class PlaceholderTransformer(private val iron: Iron) {
    private val logger = LoggerFactory.getLogger(PlaceholderTransformer::class.java)

    fun convert(
        value: Any?,
        serializationAdapter: SerializationAdapter?
    ): Any? {
        return when(value) {
            null -> null
            is Collection<*> -> convert(value.toTypedArray(), serializationAdapter)
            is Array<*> -> value.map { convert(it, serializationAdapter) }.toTypedArray()
            is ColumnSerializedField -> convertSerialized(value)
            is ColumnJsonField -> convertJson(value, serializationAdapter)
            is Enum<*> -> iron.settings.enumTransformation.serialize(value)
            is Optional<*> -> value.orElse(null)
            else -> value
        }.also {
            logger.trace(
                "Converted parameter {} {} to {} {}.",
                if (value != null) value::class.simpleName else "",
                value,
                if (it != null) it::class.simpleName else "",
                it
            )
        }
    }

    /**
     * Converts a serialized field into a value that can be used in a SQL query.
     * @param value The serialized field to convert.
     * @return The converted serialized field.
     */
    private fun convertSerialized(value: ColumnSerializedField): Any? {
        logger.trace("Deserializing parameter as a serialized field {}.", value.serializer::class.simpleName)

        val inner = value.value
            ?: return null
        value.serializer as ColumnSerializer<Any, *>

        return value.serializer.toDatabaseValue(inner)
    }

    /**
     * Converts a JSON field into a value that can be used in a SQL query.
     * @param value The JSON field to convert.
     * @param serializationAdapter The serialization adapter to use for deserialization.
     * @return The converted JSON field.
     */
    private fun convertJson(value: ColumnJsonField, serializationAdapter: SerializationAdapter?): Any? {
        checkNotNull(serializationAdapter) { "SerializationAdapter must be provided for JSON fields" }

        logger.trace("Deserializing parameter as a JSON field.")

        val inner = value.value
            ?: return null

        return serializationAdapter.serialize(inner, value.value::class.java)
    }
}