package gg.ingot.iron.serialization

/**
 * Adapt column fields to a provided type for the database.
 * @param Serialized first type to adapt for.
 * @param Deserialized second type to adapt for.
 * @author DebitCardz
 * @since 1.3
 */
interface ColumnTransformer <Serialized: Any, Deserialized: Any>
    : ColumnSerializer<Deserialized, Serialized>, ColumnDeserializer<Serialized, Deserialized>

// Internally used to denote that a column should not be transformed.
internal object EmptyTransformer : ColumnTransformer<Nothing, Nothing> {
    override fun fromDatabaseValue(value: Nothing): Nothing = error("This transformer should not be used")

    override fun toDatabaseValue(value: Nothing): Nothing = error("This transformer should not be used")
}