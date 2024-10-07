package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron

internal object CollectionValueAdapter: ValueAdapter<Collection<*>>() {
    override fun fromDatabaseValue(value: Any, iron: Iron): List<Any?> {
        if (value is Array<*>) return ArrayValueAdapter.fromDatabaseValue(value, iron).toMutableList()
        if (value !is Collection<*>) error("Expected a collection, but the database gave back ${value::class.java.name}")

        return value.map {
            iron.valueTransformer.deserialize(it)
        }
    }

    override fun toDatabaseValue(value: Collection<*>, iron: Iron): Any {
        return ArrayValueAdapter.toDatabaseValue(value.toTypedArray(), iron)
    }
}