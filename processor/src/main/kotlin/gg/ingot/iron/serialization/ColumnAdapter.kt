package gg.ingot.iron.serialization

/**
 * Adapt column fields to a provided type for the database.
 * @param Serialized first type to adapt for.
 * @param Deserialized second type to adapt for.
 * @author DebitCardz
 * @since 1.3
 */
interface ColumnAdapter <Serialized : Any, Deserialized : Any>
    : ColumnSerializer<Deserialized, Serialized>, ColumnDeserializer<Serialized, Deserialized>

// Internally used to denote that a column should not be transformed.
internal object EmptyAdapter : ColumnAdapter<Nothing, Nothing> {
    override fun fromDatabaseValue(value: Nothing): Nothing = error("This transformer should not be used")

    override fun toDatabaseValue(value: Nothing): Nothing = error("This transformer should not be used")
}

// Used for converting columns to and from json
class JsonAdapter<T: Any>(private val serializationAdapter: SerializationAdapter, private val clazz: Class<T>) :
    ColumnAdapter<String, T> {
    override fun fromDatabaseValue(value: String): T {
        return serializationAdapter.deserialize(value, clazz) as T
    }

    override fun toDatabaseValue(value: T): String {
        TODO("Not yet implemented")
    }
}