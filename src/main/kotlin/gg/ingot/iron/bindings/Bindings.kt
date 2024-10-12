package gg.ingot.iron.bindings

import gg.ingot.iron.Iron
import gg.ingot.iron.models.SqlTable

interface Bindings {

    fun bindings(instance: Any, iron: Iron): SqlBindings {
        val table = SqlTable.get(instance::class.java)
            ?: error("The class '${instance::class.qualifiedName}' does not have the @Model annotation, couldn't get bindings.")

        val mapping = table.columns.associate {
            it.variable to it.value(instance)
        }.toMutableMap()

        return SqlBindings(mapping)
    }

    // TODO: controller support
//    companion object {
//        @JvmStatic
//        @JvmName("get")
//        fun get(model: Any, iron: Iron): SqlBindings {
//            val reader = iron.modelReader
//            val entityModel = reader.read(this::class.java)
//
//            val mapping = entityModel.fields.associate {
//                it.field.variable to it.value(model)
//            }.toMutableMap()
//
//            return SqlBindings(mapping)
//        }
//    }

}