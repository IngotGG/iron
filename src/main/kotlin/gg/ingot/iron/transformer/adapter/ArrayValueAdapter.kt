package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityField

internal object ArrayValueAdapter: ValueAdapter<Array<*>>() {
    override fun fromDatabaseValue(value: Any, iron: Iron, field: EntityField): Array<*> {
        val array = when (value) {
            is Array<*> -> value
            is Collection<*> -> value.toTypedArray()
            else -> {
                error("Field '${field.name}' is an array the database gave back ${value::class.java.name}")
            }
        }

        return convertListToArray(array.map {
            iron.valueTransformer.deserialize(it, field.copy(
                isArray = false,
                isCollection = false,
            ))
        }, field.getUnderlyingType())
    }

    override fun toDatabaseValue(value: Array<*>, iron: Iron, field: EntityField): Any {
        return convertListToArray(value.map {
            iron.valueTransformer.serialize(it, field.copy(
                isArray = false,
                isCollection = false,
            ))
        }, field.getUnderlyingType())
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertListToArray(list: List<*>, type: Class<*>): Array<*> {
        val array = java.lang.reflect.Array.newInstance(type, list.size) as Array<Any?>
        for (i in list.indices) array[i] = list[i]
        return array
    }
}