package gg.ingot.iron.bindings

import gg.ingot.iron.Iron
import gg.ingot.iron.models.SqlTable

interface Bindings {

    fun bindings(): SqlBindings {
        return SqlBindings().with(this)
    }

    companion object {
        @JvmStatic
        @JvmName("get")
        fun of(model: Any, iron: Iron): SqlBindings {
            val table = SqlTable.get(model::class.java)
                ?: error("The class '${model::class.qualifiedName}' does not have the @Model annotation, couldn't get bindings.")

            val mapping = table.columns.associate {
                it.variable to iron.resultMapper.serialize(it, it.value(model))
            }.toMutableMap()

            return SqlBindings(mapping)
        }
    }

}