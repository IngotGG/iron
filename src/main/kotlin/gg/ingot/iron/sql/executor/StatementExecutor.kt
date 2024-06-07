package gg.ingot.iron.sql.executor

import gg.ingot.iron.sql.MappedResultSet
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import kotlin.reflect.KClass

interface StatementExecutor {
    fun <T : Any?> transaction(block: StatementExecutor.() -> T): T

    fun execute(@Language("SQL") statement: String): Boolean

    fun query(@Language("SQL") query: String): ResultSet

    fun <T : Any> queryMapped(@Language("SQL") query: String, clazz: KClass<T>): MappedResultSet<T>

    fun prepare(@Language("SQL") statement: String, vararg values: Any): ResultSet?

    fun <T : Any> prepareMapped(@Language("SQL") statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T>
}

@JvmName("queryInline")
inline fun <reified T : Any> StatementExecutor.queryMapped(@Language("SQL") query: String): MappedResultSet<T> {
    return queryMapped(query, T::class)
}

@JvmName("prepareInline")
inline fun <reified T: Any> StatementExecutor.prepare(@Language("SQL") statement: String, vararg values: Any): MappedResultSet<T> {
    return prepareMapped(statement, T::class, *values)
}
