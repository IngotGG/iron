package gg.ingot.iron.sql

import gg.ingot.iron.DBMS
import gg.ingot.iron.sql.builder.SqlBuilder
import gg.ingot.iron.sql.builder.SqlStatement
import gg.ingot.iron.sql.expressions.Entrypoint
import gg.ingot.iron.sql.scopes.Scope
import org.intellij.lang.annotations.Language

/**
 * The entrypoint for the SQL DSL
 */
open class Sql internal constructor(
    val driver: DBMS,
    internal val builder: SqlBuilder
): Scope {
    internal fun <S : Sql> modify(next: S, block: SqlBuilder.() -> Unit): S {
        builder.block()
        return next
    }

    /**
     * Gets the compiled SQL statements from the sql builder.
     * @return The compiled SQL statements.
     */
    fun statements(): List<SqlStatement> {
        return builder.statements()
    }

    override fun toString(): String {
        return builder.toString()
    }

    companion object {
        fun of(@Language("SQL") raw: String, driver: DBMS = DBMS.UNKNOWN): Sql {
            return Sql(driver, SqlBuilder(raw))
        }
    }
}

@Suppress("FunctionName")
fun Sql(driver: DBMS): Entrypoint = Entrypoint(Sql(driver, SqlBuilder(driver)))