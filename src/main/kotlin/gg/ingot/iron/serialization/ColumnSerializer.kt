package gg.ingot.iron.serialization

/**
 * Adapt column fields to a provided type for the database.
 * @param From The type to adapt from.
 * @param To The type to adapt to.
 * @author DebitCardz
 * @since 1.3
 */
interface ColumnSerializer <From, To> {
    /**
     * Serialize the given value into the provided type.
     * @param value The value to serialize.
     * @return The serialized value.
     */
    fun toDatabaseValue(value: From): To
}

// Internally used to denote that a column should not be serialized.
internal object EmptySerializer : ColumnSerializer<Nothing, Nothing> {
    override fun toDatabaseValue(value: Nothing): Nothing = error("This serializer should not be used")
}