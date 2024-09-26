package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField

internal object EnumValueAdapter: ValueAdapter<Enum<*>>() {

    override fun toDatabaseValue(value: Enum<*>, iron: Iron, field: EntityField): Any {
        return iron.settings.enumTransformation.serialize(value)
    }

    override fun fromDatabaseValue(value: Any, iron: Iron, field: EntityField): Enum<*> {
        val enum = field.getUnderlyingType()
        return iron.settings.enumTransformation.deserialize(value, enum)
    }

}