package gg.ingot.iron

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.strategies.NamingStrategy
import org.atteo.evo.inflector.English
import java.lang.reflect.Field
import kotlin.reflect.full.primaryConstructor

class Inflector(private val iron: Iron) {

    fun tableName(input: String): String {
        return NamingStrategy.SNAKE_CASE.transform(
            English.plural(input)
        )
    }

    fun columnName(field: Field): String {
        // If this is a data class, the annotation might instead be on the constructor parameter
        val isData = field.declaringClass.kotlin.isData

        val annotation = if (isData) {
            val constructor = field.declaringClass.kotlin.primaryConstructor
            val parameter = constructor?.parameters?.firstOrNull { it.name == field.name }
            parameter?.annotations?.firstOrNull { it is Column } as Column?
        } else {
            field.annotations.firstOrNull { it is Column } as Column?
        }

        val name = annotation?.name?.takeIf { it.isNotBlank() }
            ?: field.name

        return iron.settings.namingStrategy.transform(name)
    }

}