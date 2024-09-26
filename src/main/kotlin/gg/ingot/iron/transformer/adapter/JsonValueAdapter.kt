package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField

internal object JsonValueAdapter: ValueAdapter<Any>() {

    override fun toDatabaseValue(value: Any, iron: Iron, field: EntityField): Any {
        return iron.settings.serialization?.serialize(value, field.field.java.type)
            ?: error("Serialization is not configured in Iron, either enable it or remove @Column(json = true)")
    }

    override fun fromDatabaseValue(value: Any, iron: Iron, field: EntityField): Any {
        return iron.settings.serialization?.deserialize(value, field.field.java.type)
            ?: error("Serialization is not configured in Iron, either enable it or remove @Column(json = true)")
    }

}