package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField

internal object CollectionValueAdapter: ValueAdapter<Collection<*>>() {
    override fun fromDatabaseValue(value: Any, iron: Iron, field: EntityField): List<Any?> {
        return ArrayValueAdapter.fromDatabaseValue(value, iron, field.copy(
            isArray = true,
            isCollection = false,
        )).toList()
    }

    override fun toDatabaseValue(value: Collection<*>, iron: Iron, field: EntityField): Any {
        return ArrayValueAdapter.toDatabaseValue(value.toTypedArray(), iron, field.copy(
            isArray = true,
            isCollection = false,
        ))
    }
}