package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron

internal object SetValueAdapter: ValueAdapter<Set<*>>() {
    override fun fromDatabaseValue(value: Any, iron: Iron): Set<Any?> {
        return ArrayValueAdapter.fromDatabaseValue(value, iron).toMutableSet()
    }

    override fun toDatabaseValue(value: Set<*>, iron: Iron): Any {
        return ArrayValueAdapter.toDatabaseValue(value.toTypedArray(), iron)
    }
}