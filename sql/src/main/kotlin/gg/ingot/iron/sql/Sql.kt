package gg.ingot.iron.sql

import gg.ingot.iron.DBMS
import gg.ingot.iron.sql.builder.SqlBuilder
import gg.ingot.iron.sql.expressions.Entrypoint
import org.intellij.lang.annotations.Language

open class Sql internal constructor(
    val driver: DBMS,
    internal val builder: SqlBuilder,
) {
    internal fun <S : Sql> modify(next: S, block: SqlBuilder.() -> Unit): S {
        builder.block()
        return next
    }

    override fun toString(): String {
        return "$builder;"
    }

    companion object {
        fun of(@Language("SQL") raw: String, driver: DBMS = DBMS.UNKNOWN): Sql {
            return Sql(driver, SqlBuilder(raw))
        }
    }
}

@Suppress("FunctionName")
fun Sql(driver: DBMS): Entrypoint = Entrypoint(Sql(driver, SqlBuilder()))