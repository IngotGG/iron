package gg.ingot.iron.sql.binding

import gg.ingot.iron.Iron

interface Bindings {

    fun bindings(instance: Any, iron: Iron): SqlBindings {
        val reader = iron.modelReader
        val model = reader.read(this::class.java)

        val mapping = model.fields.associate {
            it.field.variable to it.value(instance)
        }.toMutableMap()

        return SqlBindings(mapping)
    }

    companion object {
        @JvmStatic
        @JvmName("get")
        fun get(model: Any, iron: Iron): SqlBindings {
            val reader = iron.modelReader
            val entityModel = reader.read(this::class.java)

            val mapping = entityModel.fields.associate {
                it.field.variable to it.value(model)
            }.toMutableMap()

            return SqlBindings(mapping)
        }
    }

}