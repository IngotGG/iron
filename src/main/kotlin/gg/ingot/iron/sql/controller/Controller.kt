package gg.ingot.iron.sql.controller

import gg.ingot.iron.sql.MappedResultSet
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import kotlin.reflect.KClass

sealed interface Controller {
    fun <T : Any?> transaction(block: Controller.() -> T): T

    fun query(@Language("SQL") query: String): ResultSet

    fun <T : Any> query(@Language("SQL") query: String, clazz: KClass<T>): MappedResultSet<T>

    fun prepare(@Language("SQL") statement: String, vararg values: Any): ResultSet?

    fun <T : Any> prepare(@Language("SQL") statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T>

    fun execute(@Language("SQL") statement: String): Boolean
}

inline fun <reified T : Any> Controller.queryMapped(@Language("SQL") query: String) =
    query(query, T::class)

inline fun <reified T : Any> Controller.prepareMapped(@Language("SQL") statement: String, vararg values: Any) =
    prepare(statement, T::class, *values)