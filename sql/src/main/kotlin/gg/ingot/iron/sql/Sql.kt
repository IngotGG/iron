package gg.ingot.iron.sql

import gg.ingot.iron.DBMS
import gg.ingot.iron.sql.expressions.Entrypoint
import org.intellij.lang.annotations.Language

open class Sql internal constructor(
    val driver: DBMS,
    internal val builder: StringBuilder,
) {
    fun <S : Sql> modify(next: S, block: StringBuilder.() -> Unit): S {
        builder.block()
        builder.append(' ')
        return next
    }

    override fun toString(): String {
        return builder.toString().trim() + ';'
    }

    companion object {
        fun of(@Language("SQL") raw: String, driver: DBMS = DBMS.UNKNOWN): Sql {
            return Sql(driver, StringBuilder(raw))
        }
    }
}

@Suppress("FunctionName")
fun Sql(driver: DBMS): Entrypoint = Entrypoint(Sql(driver, StringBuilder()))