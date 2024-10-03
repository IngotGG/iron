package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron

internal object ArrayValueAdapter: ValueAdapter<Array<*>>() {
    override fun fromDatabaseValue(value: Any, iron: Iron): Array<*> {
        if (value is Collection<*>) return CollectionValueAdapter.fromDatabaseValue(value, iron).toTypedArray()
        if (value !is Array<*>) error("Expected an array, but the database gave back ${value::class.java.name}")

        val underlyingType = value.getUnderlyingType()
            ?: error("Failed to find the type parameters for ${value::class.java.name}")

        return convertListToArray(value.map {
            iron.valueTransformer.deserialize(it)
        }, underlyingType)
    }

    override fun toDatabaseValue(value: Array<*>, iron: Iron): Any {
        val underlyingType = value.getUnderlyingType()
            ?: error("Failed to find the type parameters for ${value::class.java.name}")

        return convertListToArray(value.map {
            iron.valueTransformer.serialize(it)
        }, underlyingType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertListToArray(list: List<*>, type: Class<*>): Array<*> {
        val array = java.lang.reflect.Array.newInstance(type, list.size) as Array<Any?>
        for (i in list.indices) array[i] = list[i]
        return array
    }
}