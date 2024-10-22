package gg.ingot.iron.sql.scopes.select.partial

import gg.ingot.iron.sql.scopes.select.GroupBySelectScope
import gg.ingot.iron.sql.types.Column

/**
 * A partial containing the methods for the `GROUP BY` clause.
 * @author santio
 * @since 2.0
 */
interface GroupByPartial {

    fun groupBy(vararg columns: String): GroupBySelectScope
    fun groupBy(vararg columns: Column): GroupBySelectScope
    infix fun groupBy(columns: List<Column>): GroupBySelectScope
    infix fun groupBy(columns: String): GroupBySelectScope
    infix fun groupBy(column: Column): GroupBySelectScope

}