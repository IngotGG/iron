package gg.ingot.iron.serialization

/**
 * Adapt column fields to a provided type.
 * @param From The type to adapt from.
 * @param To The type to adapt to.
 * @author DebitCardz
 * @since 1.3
 */
interface ColumnDeserializer <From, To> {
    /**
     * Deserialize the given value into the provided type.
     * @param value The value to deserialize.
     * @return The deserialized value.
     */
    fun deserialize(value: From): To
}

/**
 * Deserialize an enum from a string, will automatically retrieve the
 * enum type from the parameter field.
 * @param clazz The class of the enum to deserialize.
 *  @author DebitCardz
 *  @since 1.0.3
 */
internal class EnumColumnDeserializer(
    private val clazz: Class<out Enum<*>>
) : ColumnDeserializer<String, Enum<*>> {
    override fun deserialize(value: String): Enum<*> {
        return java.lang.Enum.valueOf(clazz, value)
    }
}

// Internally used to denote that a column should not be deserialized.
internal object EmptyDeserializer : ColumnDeserializer<Nothing, Nothing> {
    override fun deserialize(value: Nothing): Nothing = error("This deserializer should not be used")
}
