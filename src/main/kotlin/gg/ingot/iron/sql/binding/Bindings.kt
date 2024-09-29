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

}