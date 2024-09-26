package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField

internal object SetValueAdapter: ValueAdapter<Set<*>>() {
    override fun fromDatabaseValue(value: Any, iron: Iron, field: EntityField): Set<Any?> {
        return ArrayValueAdapter.fromDatabaseValue(value, iron, field.copy(
            isArray = true,
            isSet = false,
        )).toSet()
    }

    override fun toDatabaseValue(value: Set<*>, iron: Iron, field: EntityField): Any {
        return ArrayValueAdapter.toDatabaseValue(value.toTypedArray(), iron, field.copy(
            isArray = true,
            isSet = false,
        ))
    }
}