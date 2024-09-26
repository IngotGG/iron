package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField

abstract class ValueAdapter<T: Any> {
    abstract fun fromDatabaseValue(value: Any, iron: Iron, field: EntityField): T
    abstract fun toDatabaseValue(value: T, iron: Iron, field: EntityField): Any
}