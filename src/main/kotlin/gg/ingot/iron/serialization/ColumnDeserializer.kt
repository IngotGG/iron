package gg.ingot.iron.serialization

/**
 * Adapt column fields to a provided type from the database.
 * @param From The type to adapt from.
 * @param To The type to adapt to.
 * @author DebitCardz
 * @since 1.3
 */
interface ColumnDeserializer <From: Any, To> {
    /**
     * Deserialize the given value into the provided type.
     * @param value The value to deserialize.
     * @return The deserialized value.
     */
    fun fromDatabaseValue(value: From): To
}

// Internally used to denote that a column should not be deserialized.
internal object EmptyDeserializer : ColumnDeserializer<Nothing, Nothing> {
    override fun fromDatabaseValue(value: Nothing): Nothing = error("This deserializer should not be used")
}
