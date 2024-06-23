package gg.ingot.iron.serialization

/**
 * Adapt column fields to a provided type for the database.
 * @param T first type to adapt for.
 * @param R second type to adapt for.
 * @author DebitCardz
 * @since 1.3
 */
interface ColumnTransformer <T, R>
    : ColumnSerializer<R, T>, ColumnDeserializer<T, R>

// Internally used to denote that a column should not be transformed.
internal object EmptyTransformer : ColumnTransformer<Nothing, Nothing> {
    override fun fromDatabaseValue(value: Nothing): Nothing = error("This transformer should not be used")

    override fun toDatabaseValue(value: Nothing): Nothing = error("This transformer should not be used")
}